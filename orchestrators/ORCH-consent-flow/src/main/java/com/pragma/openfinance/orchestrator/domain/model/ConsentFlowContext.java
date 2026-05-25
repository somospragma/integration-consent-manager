package com.pragma.openfinance.orchestrator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Contexto completo del flujo de consentimiento.
 * Se almacena en Redis durante la ejecución del flujo.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConsentFlowContext {

    private UUID flowId;
    private ConsentFlowState state;

    // Consent data
    private UUID consentId;
    private String consentType;
    private List<String> permissions;
    private Instant expiresAt;

    // TPP data
    private String tppId;
    private String tppName;
    private String redirectUri;
    private String clientId;

    // PAR data
    private String requestUri;
    private Instant requestUriExpiresAt;

    // Authorization data
    private String authorizationCode;
    private String codeChallenge;
    private String codeChallengeMethod;
    private String stateParam;

    // User data (populated after SCA)
    private String userId;
    private List<String> selectedAccountIds;
    private String authenticationMethod;

    // Token data
    private String accessToken;
    private String refreshToken;
    private Instant tokenExpiresAt;

    // Flow metadata
    private Instant startedAt;
    private Instant completedAt;
    private String failureReason;
    private int retryCount;

    public static ConsentFlowContext initiate(String tppId, String clientId, String redirectUri,
                                              String consentType, List<String> permissions, Instant expiresAt) {
        return ConsentFlowContext.builder()
                .flowId(UUID.randomUUID())
                .state(ConsentFlowState.INITIATED)
                .tppId(tppId)
                .clientId(clientId)
                .redirectUri(redirectUri)
                .consentType(consentType)
                .permissions(permissions)
                .expiresAt(expiresAt)
                .stateParam(UUID.randomUUID().toString())
                .startedAt(Instant.now())
                .retryCount(0)
                .build();
    }

    public boolean isTerminal() {
        return state == ConsentFlowState.COMPLETED
                || state == ConsentFlowState.USER_REJECTED
                || state == ConsentFlowState.FAILED
                || state == ConsentFlowState.CANCELLED
                || state == ConsentFlowState.SCA_TIMEOUT;
    }
}
