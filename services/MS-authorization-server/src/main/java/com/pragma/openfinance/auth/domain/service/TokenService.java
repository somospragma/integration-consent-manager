package com.pragma.openfinance.auth.domain.service;

import com.pragma.openfinance.auth.domain.model.TokenGrant;
import com.pragma.openfinance.auth.domain.port.TokenGrantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;

/**
 * Servicio de emisión y gestión de tokens.
 * Genera JWT con certificate binding (cnf claim).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    @Value("${auth.access-token-ttl:900}")
    private int accessTokenTtl;

    @Value("${auth.refresh-token-ttl:7776000}")
    private int refreshTokenTtl;

    @Value("${auth.issuer:https://auth.openfinance.example.com}")
    private String issuer;

    private final TokenGrantRepository grantRepository;
    private final JwtService jwtService;

    @Transactional
    public Map<String, Object> issueTokensFromCode(String code, String codeVerifier,
                                                    String clientId, String redirectUri,
                                                    String clientCertThumbprint) {

        TokenGrant grant = grantRepository.findByAuthorizationCodeValue(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid authorization code"));

        // Validate code not expired
        if (Instant.now().isAfter(grant.getAuthorizationCodeExpiresAt())) {
            throw new IllegalArgumentException("Authorization code expired");
        }

        // Validate PKCE
        if (!validatePkce(codeVerifier, grant.getState())) {
            throw new IllegalArgumentException("Invalid code_verifier (PKCE)");
        }

        // Generate tokens
        String accessToken = jwtService.generateAccessToken(
                grant.getPrincipalName(), clientId, grant.getAuthorizedScopes(),
                grant.getConsentId(), clientCertThumbprint, accessTokenTtl);

        String refreshToken = UUID.randomUUID().toString();
        String idToken = jwtService.generateIdToken(grant.getPrincipalName(), clientId);

        // Update grant
        grant.setAuthorizationCodeValue(null); // Single use
        grant.setAccessTokenValue(accessToken);
        grant.setAccessTokenIssuedAt(Instant.now());
        grant.setAccessTokenExpiresAt(Instant.now().plusSeconds(accessTokenTtl));
        grant.setAccessTokenScopes(grant.getAuthorizedScopes());
        grant.setRefreshTokenValue(refreshToken);
        grant.setRefreshTokenIssuedAt(Instant.now());
        grant.setRefreshTokenExpiresAt(Instant.now().plusSeconds(refreshTokenTtl));
        grantRepository.save(grant);

        log.info("Tokens issued for client: {}, consent: {}", clientId, grant.getConsentId());

        return Map.of(
                "access_token", accessToken,
                "token_type", "Bearer",
                "expires_in", accessTokenTtl,
                "refresh_token", refreshToken,
                "scope", grant.getAuthorizedScopes(),
                "id_token", idToken
        );
    }

    @Transactional
    public Map<String, Object> issueClientCredentialsToken(String clientId, String scope,
                                                            String clientCertThumbprint) {

        String accessToken = jwtService.generateAccessToken(
                clientId, clientId, scope, null, clientCertThumbprint, accessTokenTtl);

        log.info("Client credentials token issued for: {}", clientId);

        return Map.of(
                "access_token", accessToken,
                "token_type", "Bearer",
                "expires_in", accessTokenTtl,
                "scope", scope
        );
    }

    @Transactional
    public Map<String, Object> refreshToken(String refreshTokenValue, String clientId,
                                             String clientCertThumbprint) {

        TokenGrant grant = grantRepository.findByRefreshTokenValue(refreshTokenValue)
                .orElseThrow(() -> new IllegalArgumentException("Invalid refresh token"));

        // Validate not expired
        if (Instant.now().isAfter(grant.getRefreshTokenExpiresAt())) {
            throw new IllegalArgumentException("Refresh token expired");
        }

        // Rotation: generate new tokens, invalidate old refresh
        String newAccessToken = jwtService.generateAccessToken(
                grant.getPrincipalName(), clientId, grant.getAuthorizedScopes(),
                grant.getConsentId(), clientCertThumbprint, accessTokenTtl);

        String newRefreshToken = UUID.randomUUID().toString();

        grant.setAccessTokenValue(newAccessToken);
        grant.setAccessTokenIssuedAt(Instant.now());
        grant.setAccessTokenExpiresAt(Instant.now().plusSeconds(accessTokenTtl));
        grant.setRefreshTokenValue(newRefreshToken); // Rotation
        grant.setRefreshTokenIssuedAt(Instant.now());
        grantRepository.save(grant);

        log.info("Token refreshed for client: {}, consent: {}", clientId, grant.getConsentId());

        return Map.of(
                "access_token", newAccessToken,
                "token_type", "Bearer",
                "expires_in", accessTokenTtl,
                "refresh_token", newRefreshToken,
                "scope", grant.getAuthorizedScopes()
        );
    }

    @Transactional
    public void revokeToken(String token) {
        grantRepository.findByAccessTokenValue(token).ifPresent(grant -> {
            grant.setAccessTokenValue(null);
            grant.setRefreshTokenValue(null);
            grantRepository.save(grant);
            log.info("Token revoked for grant: {}", grant.getId());
        });
        grantRepository.findByRefreshTokenValue(token).ifPresent(grant -> {
            grant.setAccessTokenValue(null);
            grant.setRefreshTokenValue(null);
            grantRepository.save(grant);
            log.info("Refresh token revoked for grant: {}", grant.getId());
        });
    }

    public Map<String, Object> introspect(String token) {
        return grantRepository.findByAccessTokenValue(token)
                .filter(g -> Instant.now().isBefore(g.getAccessTokenExpiresAt()))
                .map(grant -> Map.<String, Object>of(
                        "active", true,
                        "scope", grant.getAuthorizedScopes(),
                        "client_id", grant.getRegisteredClientId(),
                        "token_type", "Bearer",
                        "exp", grant.getAccessTokenExpiresAt().getEpochSecond(),
                        "iat", grant.getAccessTokenIssuedAt().getEpochSecond(),
                        "sub", grant.getPrincipalName(),
                        "iss", issuer,
                        "consent_id", grant.getConsentId() != null ? grant.getConsentId() : ""
                ))
                .orElse(Map.of("active", false));
    }

    private boolean validatePkce(String codeVerifier, String storedChallenge) {
        if (codeVerifier == null || storedChallenge == null) return false;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            String computed = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
            return computed.equals(storedChallenge);
        } catch (Exception e) {
            return false;
        }
    }
}
