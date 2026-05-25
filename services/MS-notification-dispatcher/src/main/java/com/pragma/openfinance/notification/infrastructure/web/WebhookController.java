package com.pragma.openfinance.notification.infrastructure.web;

import com.pragma.openfinance.notification.domain.model.Webhook;
import com.pragma.openfinance.notification.domain.model.WebhookDelivery;
import com.pragma.openfinance.notification.domain.port.WebhookDeliveryRepository;
import com.pragma.openfinance.notification.domain.port.WebhookRepository;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/webhooks")
@RequiredArgsConstructor
public class WebhookController {

    private final WebhookRepository webhookRepository;
    private final WebhookDeliveryRepository deliveryRepository;

    @PostMapping
    public ResponseEntity<Webhook> registerWebhook(
            @Valid @RequestBody RegisterWebhookRequest request,
            @RequestHeader("X-Tpp-Id") String tppId) {

        Webhook webhook = Webhook.builder()
                .tppId(tppId)
                .url(request.getUrl())
                .eventTypes(request.getEventTypes())
                .secret(UUID.randomUUID().toString())
                .build();

        webhook = webhookRepository.save(webhook);
        return ResponseEntity.status(HttpStatus.CREATED).body(webhook);
    }

    @GetMapping
    public ResponseEntity<List<Webhook>> listWebhooks(
            @RequestHeader("X-Tpp-Id") String tppId) {

        return ResponseEntity.ok(webhookRepository.findByTppId(tppId));
    }

    @GetMapping("/{webhookId}")
    public ResponseEntity<Webhook> getWebhook(@PathVariable UUID webhookId) {
        return webhookRepository.findById(webhookId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{webhookId}")
    public ResponseEntity<Void> deleteWebhook(@PathVariable UUID webhookId) {
        webhookRepository.deleteById(webhookId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{webhookId}/deliveries")
    public ResponseEntity<Page<WebhookDelivery>> getDeliveries(
            @PathVariable UUID webhookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                deliveryRepository.findByWebhookIdOrderByCreatedAtDesc(
                        webhookId, PageRequest.of(page, size)));
    }

    @Data
    public static class RegisterWebhookRequest {
        private String url;
        private List<String> eventTypes;
    }
}
