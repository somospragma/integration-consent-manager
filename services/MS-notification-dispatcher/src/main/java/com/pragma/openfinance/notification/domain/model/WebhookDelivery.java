package com.pragma.openfinance.notification.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "webhook_deliveries", indexes = {
    @Index(name = "idx_deliveries_webhook_id", columnList = "webhookId"),
    @Index(name = "idx_deliveries_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WebhookDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID deliveryId;

    @Column(nullable = false)
    private UUID webhookId;

    @Column(nullable = false, length = 100)
    private String eventType;

    @Column(nullable = false)
    private UUID consentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private DeliveryStatus status = DeliveryStatus.PENDING;

    private Integer httpStatusCode;

    @Builder.Default
    private int retryCount = 0;

    private Instant nextRetryAt;

    private Instant deliveredAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(columnDefinition = "TEXT")
    private String responseBody;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }

    public enum DeliveryStatus {
        PENDING, DELIVERED, FAILED, DEAD_LETTER
    }

    public void markDelivered(int httpStatus) {
        this.status = DeliveryStatus.DELIVERED;
        this.httpStatusCode = httpStatus;
        this.deliveredAt = Instant.now();
    }

    public void markFailed(int httpStatus, String response) {
        this.retryCount++;
        this.httpStatusCode = httpStatus;
        this.responseBody = response;

        if (this.retryCount >= 4) {
            this.status = DeliveryStatus.DEAD_LETTER;
        } else {
            this.status = DeliveryStatus.PENDING;
            this.nextRetryAt = calculateNextRetry();
        }
    }

    private Instant calculateNextRetry() {
        long delaySeconds = switch (retryCount) {
            case 1 -> 30;
            case 2 -> 300;
            case 3 -> 1800;
            default -> 3600;
        };
        return Instant.now().plusSeconds(delaySeconds);
    }
}
