# API Webhook Management — Documentación Funcional

## Propósito

Permite a TPPs registrar URLs de callback para recibir notificaciones automáticas cuando cambia el estado de un consentimiento.

---

## POST /v1/webhooks — Registrar webhook

### ¿Qué hace?

Registra una URL donde el sistema enviará notificaciones HTTP POST cuando ocurran eventos de consentimiento.

### Request

| Campo | Tipo | Obligatorio | Descripción | Reglas |
|---|---|---|---|---|
| `url` | string (uri) | Sí | URL de callback | Debe ser HTTPS. No localhost. No IPs privadas |
| `eventTypes` | array[string] | Sí | Eventos a recibir | Mínimo 1. Usar `*` para todos |

### Eventos disponibles

| Evento | Cuándo se dispara |
|---|---|
| `consent.created` | Se creó un nuevo consentimiento |
| `consent.authorized` | El usuario autorizó |
| `consent.rejected` | El usuario rechazó |
| `consent.revoked` | Se revocó (por usuario, TPP o admin) |
| `consent.expired` | Expiró por TTL |
| `consent.consumed` | Se consumió (pagos single-use) |
| `*` | Todos los eventos |

### Response (201 Created)

| Campo | Tipo | Descripción |
|---|---|---|
| `webhookId` | string (uuid) | ID del webhook |
| `url` | string | URL registrada |
| `eventTypes` | array | Eventos suscritos |
| `status` | string | ACTIVE |
| `secret` | string | Secret HMAC para verificar firma (solo se muestra una vez) |
| `createdAt` | datetime | Fecha de creación |

### Payload que recibe el webhook

Cuando ocurre un evento, se envía un POST a la URL registrada:

```json
{
  "eventId": "uuid",
  "eventType": "consent.authorized",
  "timestamp": "2026-06-01T10:00:00Z",
  "data": {
    "consentId": "uuid",
    "status": "AUTHORIZED",
    "tppId": "fintech-001",
    "previousStatus": "AWAITING_AUTHORIZATION"
  }
}
```

### Headers del webhook

| Header | Descripción |
|---|---|
| `Content-Type` | application/json |
| `X-Webhook-Signature` | `sha256={hmac}` — Firma HMAC-SHA256 del body con el secret |
| `X-Webhook-Id` | ID del evento (para deduplicación) |
| `X-Webhook-Timestamp` | Unix timestamp (para prevenir replay) |

### Cómo verificar la firma

```
1. Obtener el body raw del request
2. Calcular HMAC-SHA256(secret, body)
3. Comparar con el valor del header X-Webhook-Signature (sin el prefijo "sha256=")
4. Si coinciden → el webhook es auténtico
```

### Política de reintentos

| Intento | Delay | Acción si falla |
|---|---|---|
| 1 | Inmediato | Retry |
| 2 | 30 segundos | Retry |
| 3 | 5 minutos | Retry |
| 4 | — | Dead Letter Queue (no más reintentos) |

Si un webhook falla consistentemente (4+ entregas fallidas), su estado cambia a `FAILED`.

---

## GET /v1/webhooks — Listar webhooks

Retorna todos los webhooks registrados por el TPP autenticado.

## GET /v1/webhooks/{webhookId} — Detalle

Retorna la configuración de un webhook específico.

## DELETE /v1/webhooks/{webhookId} — Eliminar

Elimina un webhook. Deja de recibir notificaciones inmediatamente.

## GET /v1/webhooks/{webhookId}/deliveries — Historial de entregas

| Campo | Tipo | Descripción |
|---|---|---|
| `data[].deliveryId` | string | ID de la entrega |
| `data[].eventType` | string | Tipo de evento |
| `data[].status` | string | DELIVERED, FAILED, PENDING, DEAD_LETTER |
| `data[].httpStatusCode` | integer | Código HTTP de la respuesta (null si no respondió) |
| `data[].retryCount` | integer | Número de reintentos |
| `data[].createdAt` | datetime | Cuándo se intentó |

## POST /v1/webhooks/{webhookId}/test — Enviar test

Envía un payload de prueba a la URL registrada para verificar conectividad.
