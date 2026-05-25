package com.pragma.openfinance.orchestrator.infrastructure.web;

import com.pragma.openfinance.orchestrator.domain.model.ConsentFlowContext;
import com.pragma.openfinance.orchestrator.domain.service.ConsentFlowOrchestrator;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/consent-flows")
@RequiredArgsConstructor
public class ConsentFlowController {

    private final ConsentFlowOrchestrator orchestrator;

    /**
     * Iniciar un nuevo flujo de consentimiento
     */
    @PostMapping
    public ResponseEntity<ConsentFlowResponse> initiateFlow(
            @Valid @RequestBody InitiateFlowRequest request,
            @RequestHeader("X-Tpp-Id") String tppId,
            @RequestHeader("X-Client-Id") String clientId) {

        ConsentFlowContext context = orchestrator.initiateFlow(
                tppId, clientId, request.getRedirectUri(),
                request.getConsentType(), request.getPermissions(), request.getExpiresAt());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ConsentFlowResponse.from(context));
    }

    /**
     * Callback: usuario autorizó el consentimiento
     */
    @PostMapping("/{flowId}/authorize")
    public ResponseEntity<ConsentFlowResponse> authorizeFlow(
            @PathVariable UUID flowId,
            @Valid @RequestBody AuthorizeFlowRequest request) {

        ConsentFlowContext context = orchestrator.processUserAuthorization(
                flowId, request.getUserId(), request.getAccountIds(), request.getAuthMethod());

        return ResponseEntity.ok(ConsentFlowResponse.from(context));
    }

    /**
     * Callback: usuario rechazó el consentimiento
     */
    @PostMapping("/{flowId}/reject")
    public ResponseEntity<ConsentFlowResponse> rejectFlow(
            @PathVariable UUID flowId,
            @RequestBody(required = false) Map<String, String> body) {

        String reason = body != null ? body.getOrDefault("reason", "USER_REJECTED") : "USER_REJECTED";
        ConsentFlowContext context = orchestrator.processUserRejection(flowId, reason);

        return ResponseEntity.ok(ConsentFlowResponse.from(context));
    }

    /**
     * Intercambiar code por token
     */
    @PostMapping("/{flowId}/token")
    public ResponseEntity<ConsentFlowResponse> exchangeToken(
            @PathVariable UUID flowId,
            @Valid @RequestBody TokenExchangeRequest request) {

        ConsentFlowContext context = orchestrator.processTokenExchange(
                flowId, request.getCode(), request.getCodeVerifier());

        return ResponseEntity.ok(ConsentFlowResponse.from(context));
    }

    /**
     * Consultar estado del flujo
     */
    @GetMapping("/{flowId}")
    public ResponseEntity<ConsentFlowResponse> getFlowStatus(@PathVariable UUID flowId) {
        ConsentFlowContext context = orchestrator.getFlowStatus(flowId);
        return ResponseEntity.ok(ConsentFlowResponse.from(context));
    }

    // --- DTOs ---

    @Data
    public static class InitiateFlowRequest {
        private String redirectUri;
        private String consentType;
        private List<String> permissions;
        private Instant expiresAt;
    }

    @Data
    public static class AuthorizeFlowRequest {
        private String userId;
        private List<String> accountIds;
        private String authMethod;
    }

    @Data
    public static class TokenExchangeRequest {
        private String code;
        private String codeVerifier;
    }

    @Data
    public static class ConsentFlowResponse {
        private UUID flowId;
        private String state;
        private UUID consentId;
        private String requestUri;
        private String redirectUri;
        private Instant startedAt;
        private Instant completedAt;
        private String failureReason;

        public static ConsentFlowResponse from(ConsentFlowContext ctx) {
            ConsentFlowResponse r = new ConsentFlowResponse();
            r.setFlowId(ctx.getFlowId());
            r.setState(ctx.getState().name());
            r.setConsentId(ctx.getConsentId());
            r.setRequestUri(ctx.getRequestUri());
            r.setRedirectUri(ctx.getRedirectUri());
            r.setStartedAt(ctx.getStartedAt());
            r.setCompletedAt(ctx.getCompletedAt());
            r.setFailureReason(ctx.getFailureReason());
            return r;
        }
    }
}
