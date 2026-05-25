# API-consent-lifecycle

## Descripción

API REST para la gestión del ciclo de vida del consentimiento. Permite crear, consultar, actualizar estado y revocar consentimientos.

## Responsabilidad

- Crear consentimientos (accounts, payments, funds-confirmation)
- Consultar estado de un consentimiento
- Revocar consentimientos activos
- Gestionar expiración

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/v1/consents` | Crear nuevo consentimiento |
| GET | `/v1/consents/{consentId}` | Obtener detalle |
| DELETE | `/v1/consents/{consentId}` | Revocar consentimiento |
| GET | `/v1/consents/{consentId}/status` | Obtener solo el estado |

## Dependencias

- MS-consent-engine (lógica de negocio)
- MS-audit-trail (registro de operaciones)
- MS-notification-dispatcher (webhooks)

## Seguridad

- mTLS obligatorio
- Bearer Token con scope `consents`
- Rate limiting: 100 req/min por TPP
