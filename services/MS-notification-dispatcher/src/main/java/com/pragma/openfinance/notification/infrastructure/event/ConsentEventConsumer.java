package com.pragma.openfinance.notification.infrastructure.event;

import com.pragma.openfinance.notification.domain.service.WebhookDispatchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentEventConsumer {

    private final WebhookDispatchService dispatchService;

    @KafkaListener(topics = "consent-events", groupId = "notification-dispatcher")
    public void handleConsentEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            UUID consentId = UUID.fromString((String) event.get("consentId"));
            String tppId = (String) event.get("tppId");

            log.info("Dispatching webhooks for event: {} consent: {} tpp: {}",
                    eventType, consentId, tppId);

            dispatchService.dispatchEvent(eventType, consentId, tppId, event);

        } catch (Exception e) {
            log.error("Failed to dispatch webhook for event: {}", event, e);
        }
    }
}
