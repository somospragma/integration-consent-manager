package com.pragma.openfinance.auth.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PushedAuthorizationRequest {

    private String requestUri;
    private String clientId;
    private String responseType;
    private String redirectUri;
    private String scope;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String consentId;
    private String state;
    private Instant expiresAt;
    private Map<String, Object> additionalParams;

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
