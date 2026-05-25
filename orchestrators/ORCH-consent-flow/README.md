# ORCH-consent-flow

## Descripción

Orquestador del flujo completo de consentimiento de cuentas (Account Access). Coordina la interacción entre el TPP, el Authorization Server, el Consent Manager y el usuario final.

## Responsabilidad

- Orquestar el flujo end-to-end de consentimiento de cuentas
- Coordinar PAR → Authorize → SCA → Consent Authorization → Token
- Gestionar timeouts y estados intermedios
- Manejar errores y rollback
- Saga pattern para consistencia eventual

## Flujo Orquestado

```
1. TPP → POST /consents (API-consent-lifecycle)
2. TPP → POST /par (Authorization Server)
3. Usuario → GET /authorize (Authorization Server)
4. Auth Server → GET /consents/{id}/validate (API-consent-authorization)
5. Usuario → SCA (Identity Provider)
6. Auth Server → POST /consents/{id}/authorize (API-consent-authorization)
7. Auth Server → Redirect con code
8. TPP → POST /token (Authorization Server)
9. Auth Server → Vincular consent_id en JWT
```

## Servicios Coordinados

| Servicio | Rol en el flujo |
|---|---|
| API-consent-lifecycle | Crear el consentimiento |
| API-consent-authorization | Validar y autorizar |
| MS-consent-engine | Lógica de estados |
| MS-audit-trail | Registrar cada paso |
| MS-notification-dispatcher | Notificar resultado |
| Authorization Server | Emitir tokens |

## Compensaciones (Saga)

| Paso fallido | Compensación |
|---|---|
| SCA timeout | Consent → REJECTED |
| Token emission fails | Consent → AWAITING (retry) |
| User cancels | Consent → REJECTED |
| System error | Consent → CREATED (retry) |

## Stack

- Java 21 + Spring Boot 3.x
- Spring State Machine (flujo de estados)
- Spring Kafka (eventos)
- Resilience4j (circuit breaker, retry)

## Configuración

| Variable | Descripción | Default |
|---|---|---|
| `FLOW_SCA_TIMEOUT_SECONDS` | Timeout para SCA | 300 |
| `FLOW_AUTH_CODE_TTL_SECONDS` | TTL del auth code | 60 |
| `FLOW_MAX_RETRIES` | Reintentos en error | 3 |
