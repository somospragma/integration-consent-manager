package com.pragma.openfinance.orchestrator.payment.domain.model;

/**
 * Estados del flujo de consentimiento de pagos.
 * Similar al flujo de cuentas pero con pasos adicionales
 * de validación de fondos y ejecución del pago.
 */
public enum PaymentConsentFlowState {
    /** Flujo iniciado - payment consent creado */
    INITIATED,
    /** PAR enviado */
    PAR_SUBMITTED,
    /** Esperando autorización del usuario */
    AWAITING_USER_AUTH,
    /** SCA completada (siempre obligatoria para pagos) */
    SCA_COMPLETED,
    /** Consentimiento de pago autorizado */
    CONSENT_AUTHORIZED,
    /** Token emitido con scope payments */
    TOKEN_ISSUED,
    /** Verificación de fondos en progreso */
    FUNDS_CHECK_IN_PROGRESS,
    /** Fondos confirmados */
    FUNDS_CONFIRMED,
    /** Fondos insuficientes */
    FUNDS_INSUFFICIENT,
    /** Pago en ejecución */
    PAYMENT_EXECUTING,
    /** Pago aceptado por el banco */
    PAYMENT_ACCEPTED,
    /** Pago completado exitosamente */
    PAYMENT_COMPLETED,
    /** Pago rechazado por el banco */
    PAYMENT_REJECTED,
    /** Consent consumido (single-use) */
    CONSENT_CONSUMED,
    /** Flujo completado */
    COMPLETED,
    /** Usuario rechazó */
    USER_REJECTED,
    /** Timeout */
    SCA_TIMEOUT,
    /** Error */
    FAILED
}
