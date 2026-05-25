# API-payment-consent-flow

**BIAN Domain:** Payment Initiation + Customer Access Entitlement

## Descripción

API del orquestador de flujo de consentimiento de pagos. Incluye pasos adicionales de verificación de fondos y ejecución del pago. El consent es single-use (se consume después del pago).

## Endpoints

| # | Operación | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 1 | Initiate | POST | `/v1/payment-consent-flows` | Iniciar flujo de pago |
| 2 | Execute | POST | `/v1/payment-consent-flows/{flowId}/authorize` | Usuario autorizó pago |
| 3 | Execute | POST | `/v1/payment-consent-flows/{flowId}/check-funds` | Verificar fondos |
| 4 | Execute | POST | `/v1/payment-consent-flows/{flowId}/execute` | Ejecutar pago |
| 5 | Retrieve | GET | `/v1/payment-consent-flows/{flowId}` | Estado del flujo/pago |

## Seguridad

- SCA siempre obligatoria (sin excepciones para pagos)
- Idempotency key requerida en execute
- Consent single-use (se consume post-pago)
- Validación ISO 20022: amount, currency, account format
- Circuit breaker en llamadas a core banking

## Dependencias

- MS-consent-engine (crear/autorizar/consumir consent)
- Payment Initiation Service (ejecutar pago)
- Core Banking (verificar fondos)
- MS-audit-trail (registrar cada paso)

## Implementación

Servicio: ORCH-payment-consent
