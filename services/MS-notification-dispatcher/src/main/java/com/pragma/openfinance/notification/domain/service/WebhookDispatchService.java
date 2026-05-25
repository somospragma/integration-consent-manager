package com.pragma.openfinance.notification.domain.service;

import com.pragma.openfinance.notification.domain.model.Webhook;
import com.pragma.openfinance.notification.domain.model.WebhookDelivery;
import com.pragma.openfinance.notification.domain.port.WebhookDeliveryRepository;
import com.pragma.openfinance.notification.domain.port.WebhookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookDispatchService {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;
    private final WebClient.Builder webClientBuilder;

    @Transactional
    public void dispatchEvent(String eventType, UUID consentId, String tppId, Map<String, Object> eventData) {
        List<Webhook> webhooks = webhookRepository.findByTppIdAndStatus(tppId, Webhook.WebhookStatus.ACTIVE);

        for (Webhook webhook : webhooks) {
            if (!webhook.getEventTypes().contains(eventType) && !webhook.getEventTypes().contains("*")) {
                continue;
            }

            WebhookDelivery delivery = WebhookDelivery.builder()
                    .webhookId(webhook.getWebhookId())
                    .eventType(eventType)
                    .consentId(consentId)
                    .build();

            delivery = deliveryRepository.save(delivery);
            sendWebhook(webhook, delivery, eventData);
        }
    }

    private void sendWebhook(Webhook webhook, WebhookDelivery delivery, Map<String, Object> payload) {
        String payloadJson = serializePayload(payload);
        String signature = computeHmacSignature(payloadJson, webhook.getSecret());

        webClientBuilder.build()
                .post()
                .uri(webhook.getUrl())
                .header("Content-Type", "application/json")
                .header("X-Webhook-Signature", "sha256=" + signature)
                .header("X-Webhook-Id", delivery.getDeliveryId().toString())
                .header("X-Webhook-Timestamp", String.valueOf(Instant.now().getEpochSecond()))
                .bodyValue(payloadJson)
                .retrieve()
                .toBodilessEntity()
                .doOnSuccess(response -> {
                    delivery.markDelivered(response.getStatusCode().value());
                    deliveryRepository.save(delivery);
                    log.info("Webhook delivered: {} to {}", delivery.getDeliveryId(), webhook.getUrl());
                })
                .doOnError(error -> {
                    delivery.markFailed(0, error.getMessage());
                    deliveryRepository.save(delivery);
                    log.warn("Webhook delivery failed: {} to {}: {}",
                            delivery.getDeliveryId(), webhook.getUrl(), error.getMessage());
                })
                .onErrorResume(e -> Mono.empty())
                .subscribe();
    }

    private String computeHmacSignature(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute HMAC signature", e);
        }
    }

    private String serializePayload(Map<String, Object> payload) {
        // Simplified - in production use Jackson ObjectMapper
        StringBuilder sb = new StringBuilder("{");
        payload.forEach((k, v) -> sb.append("\"").append(k).append("\":\"").append(v).append("\","));
        if (!payload.isEmpty()) sb.setLength(sb.length() - 1);
        sb.append("}");
        return sb.toString();
    }
}
