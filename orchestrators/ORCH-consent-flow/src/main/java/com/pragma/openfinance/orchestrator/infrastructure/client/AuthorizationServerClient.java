package com.pragma.openfinance.orchestrator.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Cliente HTTP para comunicarse con el Authorization Server.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthorizationServerClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.authorization-server.url:http://authorization-server:8080}")
    private String authServerUrl;

    /**
     * Enviar Pushed Authorization Request (PAR)
     */
    public PARResponse pushAuthorizationRequest(String clientId, String redirectUri,
                                                 String scope, String consentId,
                                                 String codeChallenge) {
        log.debug("Sending PAR for client: {}, consent: {}", clientId, consentId);

        Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(authServerUrl + "/par")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(String.join("&",
                        "response_type=code",
                        "client_id=" + clientId,
                        "redirect_uri=" + redirectUri,
                        "scope=" + scope,
                        "code_challenge=" + codeChallenge,
                        "code_challenge_method=S256",
                        "claims={\"id_token\":{\"openbanking_intent_id\":{\"value\":\"" + consentId + "\"}}}"
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return new PARResponse(
                (String) response.get("request_uri"),
                Instant.now().plusSeconds(((Number) response.get("expires_in")).longValue())
        );
    }

    /**
     * Intercambiar authorization code por tokens
     */
    public TokenResponse exchangeCodeForToken(String clientId, String code,
                                               String redirectUri, String codeVerifier) {
        log.debug("Exchanging code for token, client: {}", clientId);

        Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(authServerUrl + "/token")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .bodyValue(String.join("&",
                        "grant_type=authorization_code",
                        "code=" + code,
                        "redirect_uri=" + redirectUri,
                        "code_verifier=" + codeVerifier,
                        "client_id=" + clientId
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return new TokenResponse(
                (String) response.get("access_token"),
                (String) response.get("refresh_token"),
                (String) response.get("id_token"),
                Instant.now().plusSeconds(((Number) response.get("expires_in")).longValue())
        );
    }

    public record PARResponse(String requestUri, Instant expiresAt) {}

    public record TokenResponse(String accessToken, String refreshToken, String idToken, Instant expiresAt) {}
}
