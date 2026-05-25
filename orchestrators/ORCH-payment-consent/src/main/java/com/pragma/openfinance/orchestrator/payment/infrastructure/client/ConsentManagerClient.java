package com.pragma.openfinance.orchestrator.payment.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConsentManagerClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.consent-manager.url:http://ms-consent-engine:8080}")
    private String consentManagerUrl;

    public UUID createPaymentConsent(String tppId, BigDecimal amount, String currency,
                                     String creditorAccountId, String creditorName) {

        Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(consentManagerUrl + "/v1/consents")
                .header("X-Tpp-Id", tppId)
                .header("X-Fapi-Interaction-Id", UUID.randomUUID().toString())
                .bodyValue(Map.of(
                        "data", Map.of(
                                "type", "PAYMENTS",
                                "permissions", List.of("INITIATE_PAYMENT", "READ_PAYMENT_STATUS"),
                                "expiresAt", Instant.now().plus(24, ChronoUnit.HOURS).toString()
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

    public void consumeConsent(UUID consentId) {
        webClientBuilder.build()
                .post()
                .uri(consentManagerUrl + "/v1/consents/{id}/consume", consentId)
                .header("X-Correlation-Id", UUID.randomUUID().toString())
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
