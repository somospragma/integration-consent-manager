package com.pragma.openfinance.auth.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "registered_clients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RegisteredClient {

    @Id
    private String id;

    @Column(nullable = false, unique = true)
    private String clientId;

    @Column(nullable = false)
    private Instant clientIdIssuedAt;

    private String clientName;

    @Column(nullable = false)
    private String clientAuthenticationMethods;

    @Column(nullable = false)
    private String authorizationGrantTypes;

    private String redirectUris;

    @Column(nullable = false)
    private String scopes;

    @Column(columnDefinition = "TEXT")
    private String tokenSettings;

    @Column(columnDefinition = "TEXT")
    private String clientSettings;

    private String softwareId;

    @Column(columnDefinition = "TEXT")
    private String softwareStatement;

    @Column(nullable = false)
    @Builder.Default
    private String status = "ACTIVE";

    @Column(nullable = false)
    private Instant createdAt;

    private Instant updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
        if (clientIdIssuedAt == null) clientIdIssuedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() { updatedAt = Instant.now(); }
}
