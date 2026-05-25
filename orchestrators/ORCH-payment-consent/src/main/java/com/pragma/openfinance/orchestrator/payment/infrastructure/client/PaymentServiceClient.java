package com.pragma.openfinance.orchestrator.payment.infrastructure.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Cliente para comunicarse con el servicio de Payment Initiation
 * y el Core Banking para ejecución de pagos.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${services.payment-initiation.url:http://payment-initiation:8080}")
    private String paymentServiceUrl;

    /**
     * Confirmar disponibilidad de fondos
     */
    public boolean confirmFunds(String debtorAccountId, BigDecimal amount, String currency) {
        log.debug("Checking funds: account={}, amount={} {}", debtorAccountId, amount, currency);

        Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(paymentServiceUrl + "/v1/funds-confirmation")
                .bodyValue(Map.of(
                        "debtorAccount", debtorAccountId,
                        "amount", amount.toString(),
                        "currency", currency
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        return Boolean.TRUE.equals(response.get("fundsAvailable"));
    }

    /**
     * Ejecutar pago en core banking
     */
    public PaymentResult executePayment(String debtorAccountId, String creditorAccountId,
                                         BigDecimal amount, String currency,
                                         String remittanceReference, String consentId) {

        log.info("Executing payment: debtor={}, creditor={}, amount={} {}",
                debtorAccountId, creditorAccountId, amount, currency);

        Map<String, Object> response = webClientBuilder.build()
                .post()
                .uri(paymentServiceUrl + "/v1/domestic-payments")
                .header("X-Consent-Id", consentId)
                .header("X-Idempotency-Key", UUID.randomUUID().toString())
                .bodyValue(Map.of(
                        "data", Map.of(
                                "initiation", Map.of(
                                        "instructionIdentification", UUID.randomUUID().toString(),
                                        "endToEndIdentification", remittanceReference,
                                        "instructedAmount", Map.of(
                                                "amount", amount.toString(),
                                                "currency", currency
                                        ),
                                        "debtorAccount", Map.of(
                                                "identification", debtorAccountId
                                        ),
                                        "creditorAccount", Map.of(
                                                "identification", creditorAccountId
                                        ),
                                        "remittanceInformation", Map.of(
                                                "reference", remittanceReference
                                        )
                                )
                        )
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");

        return new PaymentResult(
                (String) data.get("domesticPaymentId"),
                (String) data.get("status"),
                (String) data.getOrDefault("transactionId", null)
        );
    }

    /**
     * Consultar estado de un pago
     */
    public String getPaymentStatus(String paymentId) {
        Map<String, Object> response = webClientBuilder.build()
                .get()
                .uri(paymentServiceUrl + "/v1/domestic-payments/{id}", paymentId)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) response.get("data");
        return (String) data.get("status");
    }

    public record PaymentResult(String paymentId, String status, String transactionId) {}
}
