package com.pragma.openfinance.orchestrator.payment.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Contexto del flujo de consentimiento de pagos.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentConsentContext {

    private UUID flowId;
    private PaymentConsentFlowState state;

    // Consent
    private UUID consentId;

    // TPP
    private String tppId;
    private String clientId;
    private String redirectUri;

    // Payment details
    private BigDecimal amount;
    private String currency;
    private String creditorAccountId;
    private String creditorName;
    private String debtorAccountId;
    private String remittanceReference;
    private String paymentType; // DOMESTIC, INTERNATIONAL, SCHEDULED

    // Authorization
    private String requestUri;
    private String authorizationCode;
    private String userId;
    private String authenticationMethod;

    // Token
    private String accessToken;
    private String refreshToken;

    // Payment execution
    private String paymentId;
    private String paymentStatus;
    private String transactionId;
    private boolean fundsChecked;
    private boolean fundsAvailable;

    // Flow metadata
    private Instant startedAt;
    private Instant completedAt;
    private String failureReason;
    private int retryCount;

    public static PaymentConsentContext initiate(String tppId, String clientId, String redirectUri,
                                                  BigDecimal amount, String currency,
                                                  String creditorAccountId, String creditorName,
                                                  String remittanceReference, String paymentType) {
        return PaymentConsentContext.builder()
                .flowId(UUID.randomUUID())
                .state(PaymentConsentFlowState.INITIATED)
                .tppId(tppId)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .amount(amount)
                .currency(currency)
                .creditorAccountId(creditorAccountId)
                .creditorName(creditorName)
                .remittanceReference(remittanceReference)
                .paymentType(paymentType)
                .startedAt(Instant.now())
                .retryCount(0)
                .build();
    }

    public boolean isTerminal() {
        return state == PaymentConsentFlowState.COMPLETED
                || state == PaymentConsentFlowState.USER_REJECTED
                || state == PaymentConsentFlowState.FAILED
                || state == PaymentConsentFlowState.PAYMENT_REJECTED
                || state == PaymentConsentFlowState.SCA_TIMEOUT
                || state == PaymentConsentFlowState.FUNDS_INSUFFICIENT;
    }
}
