package com.pragma.openfinance.orchestrator.payment.domain.service;

import com.pragma.openfinance.orchestrator.payment.domain.model.PaymentConsentContext;
import com.pragma.openfinance.orchestrator.payment.domain.model.PaymentConsentFlowState;
import com.pragma.openfinance.orchestrator.payment.infrastructure.client.ConsentManagerClient;
import com.pragma.openfinance.orchestrator.payment.infrastructure.client.PaymentServiceClient;
import com.pragma.openfinance.orchestrator.payment.infrastructure.store.PaymentFlowContextStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orquestador del flujo de consentimiento de pagos.
 * Diferencias clave con el flujo de cuentas:
 * - SCA siempre obligatoria (sin excepciones)
 * - Consent es single-use (se consume después del pago)
 * - Incluye verificación de fondos
 * - Incluye ejecución del pago
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentConsentOrchestrator {

    private final ConsentManagerClient consentManagerClient;
    private final PaymentServiceClient paymentServiceClient;
    private final PaymentFlowContextStore flowStore;

    /**
     * Paso 1: Crear consentimiento de pago
     */
    @CircuitBreaker(name = "consentManager")
    @Retry(name = "consentManager")
    public PaymentConsentContext initiatePaymentConsent(String tppId, String clientId, String redirectUri,
                                                        BigDecimal amount, String currency,
                                                        String creditorAccountId, String creditorName,
                                                        String remittanceReference, String paymentType) {

        log.info("Initiating payment consent: TPP={}, amount={} {}", tppId, amount, currency);

        PaymentConsentContext context = PaymentConsentContext.initiate(
                tppId, clientId, redirectUri, amount, currency,
                creditorAccountId, creditorName, remittanceReference, paymentType);

        // Crear consent tipo PAYMENTS en Consent Manager
        UUID consentId = consentManagerClient.createPaymentConsent(
                tppId, amount, currency, creditorAccountId, creditorName);
        context.setConsentId(consentId);

        context.setState(PaymentConsentFlowState.PAR_SUBMITTED);
        flowStore.save(context);

        log.info("Payment consent created: flow={}, consent={}", context.getFlowId(), consentId);
        return context;
    }

    /**
     * Paso 2: Usuario autorizó el pago (SCA completada)
     */
    @CircuitBreaker(name = "consentManager")
    public PaymentConsentContext processPaymentAuthorization(UUID flowId, String userId,
                                                             String debtorAccountId, String authMethod) {

        PaymentConsentContext context = flowStore.get(flowId);
        log.info("Processing payment authorization: flow={}, user={}", flowId, userId);

        context.setUserId(userId);
        context.setDebtorAccountId(debtorAccountId);
        context.setAuthenticationMethod(authMethod);
        context.setState(PaymentConsentFlowState.SCA_COMPLETED);

        // Autorizar consent
        consentManagerClient.authorizeConsent(
                context.getConsentId(), userId, List.of(debtorAccountId), authMethod);
        context.setState(PaymentConsentFlowState.CONSENT_AUTHORIZED);

        flowStore.save(context);
        return context;
    }

    /**
     * Paso 3: Verificar fondos (opcional pero recomendado)
     */
    @CircuitBreaker(name = "paymentService")
    public PaymentConsentContext checkFunds(UUID flowId) {
        PaymentConsentContext context = flowStore.get(flowId);
        log.info("Checking funds for flow: {}", flowId);

        context.setState(PaymentConsentFlowState.FUNDS_CHECK_IN_PROGRESS);

        boolean fundsAvailable = paymentServiceClient.confirmFunds(
                context.getDebtorAccountId(), context.getAmount(), context.getCurrency());

        context.setFundsChecked(true);
        context.setFundsAvailable(fundsAvailable);

        if (fundsAvailable) {
            context.setState(PaymentConsentFlowState.FUNDS_CONFIRMED);
            log.info("Funds confirmed for flow: {}", flowId);
        } else {
            context.setState(PaymentConsentFlowState.FUNDS_INSUFFICIENT);
            context.setFailureReason("Insufficient funds");
            context.setCompletedAt(Instant.now());
            log.warn("Insufficient funds for flow: {}", flowId);
        }

        flowStore.save(context);
        return context;
    }

    /**
     * Paso 4: Ejecutar el pago
     */
    @CircuitBreaker(name = "paymentService")
    @Retry(name = "paymentService")
    public PaymentConsentContext executePayment(UUID flowId) {
        PaymentConsentContext context = flowStore.get(flowId);

        if (context.getState() != PaymentConsentFlowState.FUNDS_CONFIRMED
                && context.getState() != PaymentConsentFlowState.CONSENT_AUTHORIZED) {
            throw new IllegalStateException("Cannot execute payment in state: " + context.getState());
        }

        log.info("Executing payment for flow: {}", flowId);
        context.setState(PaymentConsentFlowState.PAYMENT_EXECUTING);

        // Ejecutar pago en core banking
        var paymentResult = paymentServiceClient.executePayment(
                context.getDebtorAccountId(),
                context.getCreditorAccountId(),
                context.getAmount(),
                context.getCurrency(),
                context.getRemittanceReference(),
                context.getConsentId().toString());

        context.setPaymentId(paymentResult.paymentId());
        context.setPaymentStatus(paymentResult.status());
        context.setTransactionId(paymentResult.transactionId());

        if ("ACCEPTED".equals(paymentResult.status()) || "COMPLETED".equals(paymentResult.status())) {
            context.setState(PaymentConsentFlowState.PAYMENT_ACCEPTED);

            // Consumir consent (single-use)
            consentManagerClient.consumeConsent(context.getConsentId());
            context.setState(PaymentConsentFlowState.CONSENT_CONSUMED);
            context.setState(PaymentConsentFlowState.COMPLETED);
            context.setCompletedAt(Instant.now());

            log.info("Payment completed: flow={}, paymentId={}", flowId, paymentResult.paymentId());
        } else {
            context.setState(PaymentConsentFlowState.PAYMENT_REJECTED);
            context.setFailureReason("Payment rejected: " + paymentResult.status());
            context.setCompletedAt(Instant.now());

            log.warn("Payment rejected: flow={}, reason={}", flowId, paymentResult.status());
        }

        flowStore.save(context);
        return context;
    }

    /**
     * Consultar estado del pago
     */
    public PaymentConsentContext getPaymentStatus(UUID flowId) {
        PaymentConsentContext context = flowStore.get(flowId);

        // Si el pago está en progreso, consultar estado actualizado
        if (context.getState() == PaymentConsentFlowState.PAYMENT_EXECUTING
                || context.getState() == PaymentConsentFlowState.PAYMENT_ACCEPTED) {

            String currentStatus = paymentServiceClient.getPaymentStatus(context.getPaymentId());
            context.setPaymentStatus(currentStatus);

            if ("COMPLETED".equals(currentStatus)) {
                context.setState(PaymentConsentFlowState.PAYMENT_COMPLETED);
                context.setCompletedAt(Instant.now());
                flowStore.save(context);
            }
        }

        return context;
    }
}
