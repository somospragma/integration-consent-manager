# API-consent-flow

**BIAN Domain:** Customer Access Entitlement (Orchestrated)

## Descripción

API del orquestador de flujo de consentimiento de cuentas. Expone el flujo completo como una saga: desde la creación del consent hasta la emisión del token.

## Endpoints

| # | Operación | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 1 | Initiate | POST | `/v1/consent-flows` | Iniciar flujo |
| 2 | Execute | POST | `/v1/consent-flows/{flowId}/authorize` | Usuario autorizó |
| 3 | Execute | POST | `/v1/consent-flows/{flowId}/reject` | Usuario rechazó |
| 4 | Execute | POST | `/v1/consent-flows/{flowId}/token` | Intercambiar code por token |
| 5 | Retrieve | GET | `/v1/consent-flows/{flowId}` | Estado del flujo |

## Seguridad

- mTLS + Bearer JWT
- Flujo con timeout (SCA: 5 min)
- Estado almacenado en Redis (TTL 10 min activo, 1h completado)
- Circuit breaker + retry en llamadas a servicios downstream

## Dependencias

- MS-consent-engine (crear/autorizar consent)
- Authorization Server (PAR + token exchange)
- MS-audit-trail (registrar cada paso)

## Implementación

Servicio: ORCH-consent-flow
