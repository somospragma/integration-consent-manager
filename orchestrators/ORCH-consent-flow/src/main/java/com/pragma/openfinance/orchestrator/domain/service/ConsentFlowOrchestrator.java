package com.pragma.openfinance.orchestrator.domain.service;

import com.pragma.openfinance.orchestrator.domain.model.ConsentFlowContext;
import com.pragma.openfinance.orchestrator.domain.model.ConsentFlowState;
import com.pragma.openfinance.orchestrator.infrastructure.client.AuthorizationServerClient;
import com.pragma.openfinance.orchestrator.infrastructure.client.ConsentManagerClient;
import com.pragma.openfinance.orchestrator.infrastructure.store.FlowContextStore;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Orquestador principal del flujo de consentimiento de cuentas.
 * Implementa el patrón Saga para coordinar múltiples servicios.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentFlowOrchestrator {

    private final ConsentManagerClient consentManagerClient;
    private final AuthorizationServerClient authServerClient;
    private final FlowContextStore flowContextStore;

    /**
     * Paso 1: Iniciar el flujo - Crear consentimiento y PAR
     */
    @CircuitBreaker(name = "consentManager", fallbackMethod = "handleConsentCreationFailure")
    @Retry(name = "consentManager")
    public ConsentFlowContext initiateFlow(String tppId, String clientId, String redirectUri,
                                           String consentType, List<String> permissions, Instant expiresAt) {

        log.info("Initiating consent flow for TPP: {}, type: {}", tppId, consentType);

        // Crear contexto del flujo
        ConsentFlowContext context = ConsentFlowContext.initiate(
                tppId, clientId, redirectUri, consentType, permissions, expiresAt);

        // Paso 1a: Crear consentimiento en Consent Manager
        UUID consentId = consentManagerClient.createConsent(consentType, tppId, permissions, expiresAt);
        context.setConsentId(consentId);
        context.setState(ConsentFlowState.INITIATED);

        // Paso 1b: Enviar PAR al Authorization Server
        var parResponse = authServerClient.pushAuthorizationRequest(
                clientId, redirectUri, "openid " + consentType.toLowerCase(),
                consentId.toString(), context.getCodeChallenge());

        context.setRequestUri(parResponse.requestUri());
        context.setRequestUriExpiresAt(parResponse.expiresAt());
        context.setState(ConsentFlowState.PAR_SUBMITTED);

        // Persistir contexto
        flowContextStore.save(context);

        log.info("Flow initiated: {}, consent: {}, requestUri: {}",
                context.getFlowId(), consentId, parResponse.requestUri());

        return context;
    }

    /**
     * Paso 2: Procesar callback de autorización del usuario
     * (llamado por el Authorization Server después de SCA)
     */
    @CircuitBreaker(name = "consentManager", fallbackMethod = "handleAuthorizationFailure")
    public ConsentFlowContext processUserAuthorization(UUID flowId, String userId,
                                                       List<String> selectedAccountIds,
                                                       String authMethod) {

        ConsentFlowContext context = flowContextStore.get(flowId);

        if (context.isTerminal()) {
            log.warn("Flow {} is already in terminal state: {}", flowId, context.getState());
            return context;
        }

        log.info("Processing user authorization for flow: {}, user: {}", flowId, userId);

        // Actualizar contexto con datos del usuario
        context.setUserId(userId);
        context.setSelectedAccountIds(selectedAccountIds);
        context.setAuthenticationMethod(authMethod);
        context.setState(ConsentFlowState.SCA_COMPLETED);

        // Autorizar consentimiento en Consent Manager
        consentManagerClient.authorizeConsent(context.getConsentId(), userId, selectedAccountIds, authMethod);
        context.setState(ConsentFlowState.CONSENT_AUTHORIZED);

        flowContextStore.save(context);

        log.info("Consent authorized for flow: {}, consent: {}", flowId, context.getConsentId());

        return context;
    }

    /**
     * Paso 3: Procesar rechazo del usuario
     */
    public ConsentFlowContext processUserRejection(UUID flowId, String reason) {

        ConsentFlowContext context = flowContextStore.get(flowId);

        log.info("Processing user rejection for flow: {}, reason: {}", flowId, reason);

        // Rechazar consentimiento
        consentManagerClient.rejectConsent(context.getConsentId(), reason);

        context.setState(ConsentFlowState.USER_REJECTED);
        context.setFailureReason(reason);
        context.setCompletedAt(Instant.now());

        flowContextStore.save(context);

        return context;
    }

    /**
     * Paso 4: Procesar token exchange (code → token)
     */
    @CircuitBreaker(name = "authServer", fallbackMethod = "handleTokenExchangeFailure")
    @Retry(name = "authServer")
    public ConsentFlowContext processTokenExchange(UUID flowId, String authorizationCode, String codeVerifier) {

        ConsentFlowContext context = flowContextStore.get(flowId);

        log.info("Processing token exchange for flow: {}", flowId);

        context.setAuthorizationCode(authorizationCode);
        context.setState(ConsentFlowState.CODE_ISSUED);

        // Intercambiar code por token
        var tokenResponse = authServerClient.exchangeCodeForToken(
                context.getClientId(), authorizationCode, context.getRedirectUri(), codeVerifier);

        context.setAccessToken(tokenResponse.accessToken());
        context.setRefreshToken(tokenResponse.refreshToken());
        context.setTokenExpiresAt(tokenResponse.expiresAt());
        context.setState(ConsentFlowState.TOKEN_ISSUED);

        // Flujo completado
        context.setState(ConsentFlowState.COMPLETED);
        context.setCompletedAt(Instant.now());

        flowContextStore.save(context);

        log.info("Flow completed: {}, consent: {}", flowId, context.getConsentId());

        return context;
    }

    /**
     * Procesar timeout de SCA
     */
    public ConsentFlowContext processSCATimeout(UUID flowId) {
        ConsentFlowContext context = flowContextStore.get(flowId);

        log.warn("SCA timeout for flow: {}", flowId);

        consentManagerClient.rejectConsent(context.getConsentId(), "SCA_TIMEOUT");

        context.setState(ConsentFlowState.SCA_TIMEOUT);
        context.setFailureReason("SCA timeout exceeded");
        context.setCompletedAt(Instant.now());

        flowContextStore.save(context);

        return context;
    }

    /**
     * Obtener estado actual del flujo
     */
    public ConsentFlowContext getFlowStatus(UUID flowId) {
        return flowContextStore.get(flowId);
    }

    // --- Fallback methods (compensaciones) ---

    private ConsentFlowContext handleConsentCreationFailure(String tppId, String clientId,
                                                            String redirectUri, String consentType,
                                                            List<String> permissions, Instant expiresAt,
                                                            Throwable t) {
        log.error("Failed to create consent for TPP: {}, error: {}", tppId, t.getMessage());
        ConsentFlowContext context = ConsentFlowContext.builder()
                .flowId(UUID.randomUUID())
                .state(ConsentFlowState.FAILED)
                .tppId(tppId)
                .failureReason("Consent creation failed: " + t.getMessage())
                .startedAt(Instant.now())
                .completedAt(Instant.now())
                .build();
        flowContextStore.save(context);
        return context;
    }

    private ConsentFlowContext handleAuthorizationFailure(UUID flowId, String userId,
                                                          List<String> selectedAccountIds,
                                                          String authMethod, Throwable t) {
        log.error("Failed to authorize consent for flow: {}, error: {}", flowId, t.getMessage());
        ConsentFlowContext context = flowContextStore.get(flowId);
        context.setState(ConsentFlowState.FAILED);
        context.setFailureReason("Authorization failed: " + t.getMessage());
        context.setCompletedAt(Instant.now());
        flowContextStore.save(context);
        return context;
    }

    private ConsentFlowContext handleTokenExchangeFailure(UUID flowId, String authorizationCode,
                                                          String codeVerifier, Throwable t) {
        log.error("Failed token exchange for flow: {}, error: {}", flowId, t.getMessage());
        ConsentFlowContext context = flowContextStore.get(flowId);
        context.setState(ConsentFlowState.FAILED);
        context.setFailureReason("Token exchange failed: " + t.getMessage());
        context.setCompletedAt(Instant.now());
        flowContextStore.save(context);
        return context;
    }
}
