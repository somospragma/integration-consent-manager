package com.pragma.openfinance.consent.infrastructure.event;

import com.pragma.openfinance.consent.domain.model.Consent;
import com.pragma.openfinance.consent.domain.model.ConsentEvent.ActorType;
import com.pragma.openfinance.consent.domain.model.ConsentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentEventPublisher {

    private static final String TOPIC = "consent-events";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishConsentCreated(Consent consent) {
        publish("consent.created", consent, null);
    }

    public void publishConsentAuthorized(Consent consent, ConsentStatus previousStatus) {
        publish("consent.authorized", consent, previousStatus);
    }

    public void publishConsentRejected(Consent consent, ConsentStatus previousStatus, String reason) {
        Map<String, Object> event = buildEvent("consent.rejected", consent, previousStatus);
        event.put("reason", reason);
        send(consent.getConsentId().toString(), event);
    }

    public void publishConsentRevoked(Consent consent, ConsentStatus previousStatus, String actorId, ActorType actorType) {
        Map<String, Object> event = buildEvent("consent.revoked", consent, previousStatus);
        event.put("actorId", actorId);
        event.put("actorType", actorType.name());
        send(consent.getConsentId().toString(), event);
    }

    public void publishConsentExpired(Consent consent, ConsentStatus previousStatus) {
        publish("consent.expired", consent, previousStatus);
    }

    public void publishConsentConsumed(Consent consent, ConsentStatus previousStatus) {
        publish("consent.consumed", consent, previousStatus);
    }

    private void publish(String eventType, Consent consent, ConsentStatus previousStatus) {
        Map<String, Object> event = buildEvent(eventType, consent, previousStatus);
        send(consent.getConsentId().toString(), event);
    }

    private Map<String, Object> buildEvent(String eventType, Consent consent, ConsentStatus previousStatus) {
        return new java.util.HashMap<>(Map.of(
                "eventType", eventType,
                "consentId", consent.getConsentId().toString(),
                "tppId", consent.getTppId(),
                "consentType", consent.getType().name(),
                "currentStatus", consent.getStatus().name(),
                "previousStatus", previousStatus != null ? previousStatus.name() : "NONE",
                "timestamp", Instant.now().toString()
        ));
    }

    private void send(String key, Object event) {
        kafkaTemplate.send(TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event for consent: {}", key, ex);
                    } else {
                        log.debug("Event published for consent: {}", key);
                    }
                });
    }
}
