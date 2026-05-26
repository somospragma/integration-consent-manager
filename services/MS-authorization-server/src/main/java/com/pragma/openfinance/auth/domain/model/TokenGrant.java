package com.pragma.openfinance.auth.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "oauth2_authorizations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TokenGrant {

    @Id
    private String id;

    @Column(nullable = false)
    private String registeredClientId;

    @Column(nullable = false)
    private String principalName;

    @Column(nullable = false)
    private String authorizationGrantType;

    private String authorizedScopes;
    private String state;

    @Column(columnDefinition = "TEXT")
    private String authorizationCodeValue;
    private Instant authorizationCodeIssuedAt;
    private Instant authorizationCodeExpiresAt;

    @Column(columnDefinition = "TEXT")
    private String accessTokenValue;
    private Instant accessTokenIssuedAt;
    private Instant accessTokenExpiresAt;
    private String accessTokenScopes;

    @Column(columnDefinition = "TEXT")
    private String refreshTokenValue;
    private Instant refreshTokenIssuedAt;
    private Instant refreshTokenExpiresAt;

    private String consentId;

    @Column(nullable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() { createdAt = Instant.now(); }
}
