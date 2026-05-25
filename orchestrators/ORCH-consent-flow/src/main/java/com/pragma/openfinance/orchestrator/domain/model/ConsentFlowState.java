package com.pragma.openfinance.orchestrator.domain.model;

/**
 * Estados del flujo de consentimiento de cuentas.
 * Representa la saga completa desde la solicitud hasta la emisión del token.
 */
public enum ConsentFlowState {
    /** Flujo iniciado - consent creado */
    INITIATED,
    /** PAR enviado al Authorization Server */
    PAR_SUBMITTED,
    /** Usuario redirigido a pantalla de autorización */
    AWAITING_USER_AUTH,
    /** Usuario completó SCA exitosamente */
    SCA_COMPLETED,
    /** Consentimiento autorizado por el usuario */
    CONSENT_AUTHORIZED,
    /** Authorization code emitido */
    CODE_ISSUED,
    /** Token emitido exitosamente */
    TOKEN_ISSUED,
    /** Flujo completado exitosamente */
    COMPLETED,
    /** Usuario rechazó el consentimiento */
    USER_REJECTED,
    /** Timeout en SCA */
    SCA_TIMEOUT,
    /** Error en el flujo */
    FAILED,
    /** Flujo cancelado */
    CANCELLED
}
