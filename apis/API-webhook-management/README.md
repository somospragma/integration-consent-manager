# API-webhook-management

**BIAN Domain:** Party Data Management (Notifications)

## Descripción

API para gestión de webhooks. Permite a TPPs registrar URLs de callback para recibir notificaciones cuando cambia el estado de un consentimiento.

## Endpoints

| # | Operación | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 1 | Initiate | POST | `/v1/webhooks` | Registrar webhook |
| 2 | Retrieve | GET | `/v1/webhooks` | Listar webhooks del TPP |
| 3 | Retrieve | GET | `/v1/webhooks/{webhookId}` | Detalle de un webhook |
| 4 | Execute | DELETE | `/v1/webhooks/{webhookId}` | Eliminar webhook |
| 5 | Retrieve | GET | `/v1/webhooks/{webhookId}/deliveries` | Historial de entregas |
| 6 | Execute | POST | `/v1/webhooks/{webhookId}/test` | Enviar test payload |

## Seguridad

- mTLS obligatorio
- Bearer JWT con scope `webhooks`
- TPP solo gestiona sus propios webhooks
- URL debe ser HTTPS (no localhost, no IPs privadas)
- Payload firmado con HMAC-SHA256

## Dependencias

- MS-notification-dispatcher (lógica de despacho)
- MS-consent-engine (eventos de consentimiento)
