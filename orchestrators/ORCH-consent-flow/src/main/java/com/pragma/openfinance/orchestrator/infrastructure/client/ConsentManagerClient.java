package com.pragma.openfinance.orchestrator.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Cliente HTTP para comunicarse con el Consent Manager (MS-consent-engine).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentManagerClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.consent-manager.url:http://ms-consent-engine:8080}")
    private String consentManagerUrl;

    public UUID createConsent(String type, String tppId, List<String> permissions, Instant expiresAt) {
        log.debug("Creating consent: type={}, tpp={}", type, tppId);

        Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(consentManagerUrl + "/v1/consents")
                .header("X-Tpp-Id", tppId)
                .header("X-Fapi-Interaction-Id", UUID.randomUUID().toString())
                .bodyValue(Map.of(
                        "data", Map.of(
                                "type", type,
                                "permissions", permissions,
                                "expiresAt", expiresAt.toString()
                        )
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return UUID.fromString((String) data.get("consentId"));
    }

    public void authorizeConsent(UUID consentId, String userId, List<String> accountIds, String authMethod) {
        log.debug("Authorizing consent: {}, user: {}", consentId, userId);

        webClientBuilder.build()
                .post()
                .uri(consentManagerUrl + "/v1/consents/{id}/authorize", consentId)
                .header("X-Correlation-Id", UUID.randomUUID().toString())
                .bodyValue(Map.of(
                        "userId", userId,
                        "accountIds", accountIds,
                        "authenticationMethod", authMethod
                ))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }

    public void rejectConsent(UUID consentId, String reason) {
        log.debug("Rejecting consent: {}, reason: {}", consentId, reason);

        webClientBuilder.build()
                .post()
                .uri(consentManagerUrl + "/v1/consents/{id}/reject", consentId)
                .header("X-Correlation-Id", UUID.randomUUID().toString())
                .bodyValue(Map.of("reason", reason))
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
