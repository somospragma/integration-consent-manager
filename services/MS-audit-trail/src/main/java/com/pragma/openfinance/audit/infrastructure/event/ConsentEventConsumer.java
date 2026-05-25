package com.pragma.openfinance.audit.infrastructure.event;

import com.pragma.openfinance.audit.domain.model.AuditLog;
import com.pragma.openfinance.audit.domain.service.AuditService;
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

    private final AuditService auditService;

    @KafkaListener(topics = "consent-events", groupId = "audit-trail")
    public void handleConsentEvent(Map<String, Object> event) {
        try {
            String eventType = (String) event.get("eventType");
            UUID consentId = UUID.fromString((String) event.get("consentId"));
            String tppId = (String) event.get("tppId");
            String previousStatus = (String) event.get("previousStatus");
            String currentStatus = (String) event.get("currentStatus");

            String actorId = event.containsKey("actorId")
                    ? (String) event.get("actorId")
                    : tppId;

            AuditLog.ActorType actorType = determineActorType(event);

            auditService.createAuditEntry(
                    consentId,
                    mapEventToAction(eventType),
                    actorId,
                    actorType,
                    previousStatus,
                    currentStatus,
                    null,
                    "Event: " + eventType
            );

            log.debug("Audit entry created for event: {} consent: {}", eventType, consentId);

        } catch (Exception e) {
            log.error("Failed to process consent event for audit: {}", event, e);
            // No re-throw: dead letter queue handles retries
        }
    }

    private String mapEventToAction(String eventType) {
        return switch (eventType) {
            case "consent.created" -> "CREATE";
            case "consent.authorized" -> "AUTHORIZE";
            case "consent.rejected" -> "REJECT";
            case "consent.revoked" -> "REVOKE";
            case "consent.expired" -> "EXPIRE";
            case "consent.consumed" -> "CONSUME";
            default -> "UNKNOWN";
        };
    }

    private AuditLog.ActorType determineActorType(Map<String, Object> event) {
        if (event.containsKey("actorType")) {
            return AuditLog.ActorType.valueOf((String) event.get("actorType"));
        }
        String eventType = (String) event.get("eventType");
        return switch (eventType) {
            case "consent.created" -> AuditLog.ActorType.TPP;
            case "consent.authorized", "consent.rejected" -> AuditLog.ActorType.USER;
            case "consent.expired" -> AuditLog.ActorType.SYSTEM;
            default -> AuditLog.ActorType.SYSTEM;
        };
    }
}
