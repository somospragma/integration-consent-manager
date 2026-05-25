# ORCH-payment-consent

## Descripción

Orquestador del flujo de consentimiento de pagos. Coordina la creación del consentimiento de pago, la autorización del usuario (SCA reforzada) y la vinculación con el servicio de Payment Initiation.

## Responsabilidad

- Orquestar flujo de consentimiento de pago (doméstico e internacional)
- Coordinar validación de fondos pre-autorización
- SCA reforzada para pagos (siempre 2 factores)
- Vincular consent con payment execution
- Gestionar estados de pago post-autorización
- Idempotencia en ejecución de pagos

## Flujo Orquestado

```
1. TPP → POST /domestic-payment-consents (crear consent de pago)
2. TPP → POST /par (Authorization Server con consent de pago)
3. Usuario → Authorize + SCA (obligatorio para pagos)
4. Auth Server → POST /consents/{id}/authorize
5. TPP → POST /token (obtener token con scope payments)
6. TPP → POST /domestic-payments (ejecutar pago)
7. ORCH → Verificar consent activo + tipo PAYMENT
8. ORCH → Confirmar fondos (opcional)
9. ORCH → Ejecutar pago en core banking
10. ORCH → Consent → CONSUMED
11. ORCH → Notificar resultado
```

## Diferencias con ORCH-consent-flow

| Aspecto | ORCH-consent-flow | ORCH-payment-consent |
|---|---|---|
| Tipo de consent | ACCOUNTS | PAYMENTS |
| SCA | Requerida | Siempre requerida (sin excepciones) |
| Uso del consent | Múltiple (acceso repetido) | Single-use (un pago) |
| Post-autorización | Token para acceder datos | Token para ejecutar pago |
| Funds check | No aplica | Opcional pre-ejecución |
| Estado final | AUTHORIZED (sigue activo) | CONSUMED (single-use) |

## Servicios Coordinados

| Servicio | Rol |
|---|---|
| API-consent-lifecycle | Crear consent tipo PAYMENT |
| API-consent-authorization | Autorizar consent |
| MS-consent-engine | Máquina de estados |
| MS-permission-registry | Validar permiso INITIATE_PAYMENT |
| Payment Initiation Service | Ejecutar el pago (externo) |
| Core Banking | Rails de pago |

## Stack

- Java 21 + Spring Boot 3.x
- Spring State Machine
- Spring Kafka
- Resilience4j
- OpenTelemetry (tracing del flujo completo)
