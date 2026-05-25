package com.pragma.openfinance.consent.domain.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "consents", indexes = {
    @Index(name = "idx_consents_user_id", columnList = "userId"),
    @Index(name = "idx_consents_tpp_id", columnList = "tppId"),
    @Index(name = "idx_consents_status", columnList = "status"),
    @Index(name = "idx_consents_expires_at", columnList = "expiresAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Consent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID consentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsentType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConsentStatus status;

    @Column(nullable = false, length = 100)
    private String tppId;

    @Column(length = 100)
    private String userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb", nullable = false)
    private List<String> permissions;

    @Column(nullable = false)
    private Instant expiresAt;

    private Instant authorizedAt;

    private Instant revokedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private ConsentMetadata metadata;

    @Column(length = 3, nullable = false)
    @Builder.Default
    private String countryCode = "COL";

    @Column(length = 500)
    private String redirectUrl;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (status == null) {
            status = ConsentStatus.CREATED;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isActive() {
        return status == ConsentStatus.AUTHORIZED && Instant.now().isBefore(expiresAt);
    }

    public boolean canBeAuthorized() {
        return status == ConsentStatus.AWAITING_AUTHORIZATION;
    }

    public boolean canBeRevoked() {
        return status == ConsentStatus.AUTHORIZED || status == ConsentStatus.AWAITING_AUTHORIZATION;
    }
}
