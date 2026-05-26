package com.pragma.openfinance.auth.domain.service;

import com.pragma.openfinance.auth.domain.model.PushedAuthorizationRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio para Pushed Authorization Requests (PAR) - RFC 9126.
 * Almacena los parámetros de autorización en Redis con TTL corto (60s).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PARService {

    private static final String PAR_PREFIX = "par:";

    @Value("${auth.par-ttl:60}")
    private int parTtlSeconds;

    private final RedisTemplate<String, PushedAuthorizationRequest> redisTemplate;

    public PushedAuthorizationRequest create(String clientId, String responseType, String redirectUri,
                                              String scope, String codeChallenge, String codeChallengeMethod,
                                              String consentId, String state) {

        String requestUri = "urn:pragma:par:" + UUID.randomUUID();

        PushedAuthorizationRequest par = PushedAuthorizationRequest.builder()
                .requestUri(requestUri)
                .clientId(clientId)
                .responseType(responseType)
                .redirectUri(redirectUri)
                .scope(scope)
                .codeChallenge(codeChallenge)
                .codeChallengeMethod(codeChallengeMethod)
                .consentId(consentId)
                .state(state)
                .expiresAt(Instant.now().plusSeconds(parTtlSeconds))
                .build();

        redisTemplate.opsForValue().set(PAR_PREFIX + requestUri, par, Duration.ofSeconds(parTtlSeconds));

        log.info("PAR created: {} for client: {}, consent: {}", requestUri, clientId, consentId);
        return par;
    }

    public PushedAuthorizationRequest retrieve(String requestUri) {
        PushedAuthorizationRequest par = redisTemplate.opsForValue().get(PAR_PREFIX + requestUri);
        if (par == null) {
            throw new IllegalArgumentException("PAR not found or expired: " + requestUri);
        }
        if (par.isExpired()) {
            redisTemplate.delete(PAR_PREFIX + requestUri);
            throw new IllegalArgumentException("PAR expired: " + requestUri);
        }
        // Single use - delete after retrieval
        redisTemplate.delete(PAR_PREFIX + requestUri);
        return par;
    }
}
