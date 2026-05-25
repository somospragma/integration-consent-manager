package com.pragma.openfinance.orchestrator.payment.infrastructure.web;

import com.pragma.openfinance.orchestrator.payment.domain.model.PaymentConsentContext;
import com.pragma.openfinance.orchestrator.payment.domain.service.PaymentConsentOrchestrator;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/v1/payment-consent-flows")
@RequiredArgsConstructor
public class PaymentConsentFlowController {

    private final PaymentConsentOrchestrator orchestrator;

    /**
     * Iniciar flujo de consentimiento de pago
     */
    @PostMapping
    public ResponseEntity<PaymentFlowResponse> initiatePaymentFlow(
            @Valid @RequestBody InitiatePaymentRequest request,
            @RequestHeader("X-Tpp-Id") String tppId,
            @RequestHeader("X-Client-Id") String clientId) {

        PaymentConsentContext context = orchestrator.initiatePaymentConsent(
                tppId, clientId, request.getRedirectUri(),
                request.getAmount(), request.getCurrency(),
                request.getCreditorAccountId(), request.getCreditorName(),
                request.getRemittanceReference(), request.getPaymentType());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentFlowResponse.from(context));
    }

    /**
     * Usuario autorizó el pago
     */
    @PostMapping("/{flowId}/authorize")
    public ResponseEntity<PaymentFlowResponse> authorizePayment(
            @PathVariable UUID flowId,
            @Valid @RequestBody AuthorizePaymentRequest request) {

        PaymentConsentContext context = orchestrator.processPaymentAuthorization(
                flowId, request.getUserId(), request.getDebtorAccountId(), request.getAuthMethod());

        return ResponseEntity.ok(PaymentFlowResponse.from(context));
    }

    /**
     * Verificar fondos
     */
    @PostMapping("/{flowId}/check-funds")
    public ResponseEntity<PaymentFlowResponse> checkFunds(@PathVariable UUID flowId) {
        PaymentConsentContext context = orchestrator.checkFunds(flowId);
        return ResponseEntity.ok(PaymentFlowResponse.from(context));
    }

    /**
     * Ejecutar el pago
     */
    @PostMapping("/{flowId}/execute")
    public ResponseEntity<PaymentFlowResponse> executePayment(@PathVariable UUID flowId) {
        PaymentConsentContext context = orchestrator.executePayment(flowId);
        return ResponseEntity.ok(PaymentFlowResponse.from(context));
    }

    /**
     * Consultar estado del flujo/pago
     */
    @GetMapping("/{flowId}")
    public ResponseEntity<PaymentFlowResponse> getStatus(@PathVariable UUID flowId) {
        PaymentConsentContext context = orchestrator.getPaymentStatus(flowId);
        return ResponseEntity.ok(PaymentFlowResponse.from(context));
    }

    // --- DTOs ---

    @Data
    public static class InitiatePaymentRequest {
        private String redirectUri;
        private BigDecimal amount;
        private String currency;
        private String creditorAccountId;
        private String creditorName;
        private String remittanceReference;
        private String paymentType;
    }

    @Data
    public static class AuthorizePaymentRequest {
        private String userId;
        private String debtorAccountId;
        private String authMethod;
    }

    @Data
    public static class PaymentFlowResponse {
        private UUID flowId;
        private String state;
        private UUID consentId;
        private String paymentId;
        private String paymentStatus;
        private String transactionId;
        private BigDecimal amount;
        private String currency;
        private boolean fundsAvailable;
        private Instant startedAt;
        private Instant completedAt;
        private String failureReason;

        public static PaymentFlowResponse from(PaymentConsentContext ctx) {
            PaymentFlowResponse r = new PaymentFlowResponse();
            r.setFlowId(ctx.getFlowId());
            r.setState(ctx.getState().name());
            r.setConsentId(ctx.getConsentId());
            r.setPaymentId(ctx.getPaymentId());
            r.setPaymentStatus(ctx.getPaymentStatus());
            r.setTransactionId(ctx.getTransactionId());
            r.setAmount(ctx.getAmount());
            r.setCurrency(ctx.getCurrency());
            r.setFundsAvailable(ctx.isFundsAvailable());
            r.setStartedAt(ctx.getStartedAt());
            r.setCompletedAt(ctx.getCompletedAt());
            r.setFailureReason(ctx.getFailureReason());
            return r;
        }
    }
}
