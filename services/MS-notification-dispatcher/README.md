# MS-notification-dispatcher

## Descripción

Microservicio responsable de despachar notificaciones (webhooks) a los TPPs cuando cambia el estado de un consentimiento. Implementa retry con backoff exponencial y dead letter queue.

## Responsabilidad

- Gestionar registro de webhooks por TPP
- Consumir eventos de consentimiento (Kafka)
- Despachar HTTP POST a URLs registradas
- Retry con backoff exponencial (3 intentos)
- Dead Letter Queue para entregas fallidas
- Firma HMAC de cada payload (verificable por TPP)

## Eventos Consumidos (Kafka)

| Topic | Evento | Acción |
|---|---|---|
| `consent-events` | `consent.authorized` | Notificar al TPP |
| `consent-events` | `consent.revoked` | Notificar al TPP |
| `consent-events` | `consent.expired` | Notificar al TPP |
| `consent-events` | `consent.consumed` | Notificar al TPP |

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/v1/webhooks` | Registrar webhook |
| GET | `/v1/webhooks` | Listar webhooks del TPP |
| GET | `/v1/webhooks/{id}` | Detalle de un webhook |
| DELETE | `/v1/webhooks/{id}` | Eliminar webhook |
| GET | `/v1/webhooks/{id}/deliveries` | Historial de entregas |
| POST | `/v1/webhooks/{id}/test` | Enviar test payload |

## Payload del Webhook

```json
{
  "eventId": "uuid",
  "eventType": "consent.authorized",
  "timestamp": "2026-06-01T10:00:00Z",
  "data": {
    "consentId": "uuid",
    "status": "AUTHORIZED",
    "tppId": "tpp-123"
  }
}
```

## Headers del Webhook

```
Content-Type: application/json
X-Webhook-Signature: sha256=hmac-signature
X-Webhook-Id: event-uuid
X-Webhook-Timestamp: 1717200000
```

## Retry Policy

| Intento | Delay | Acción si falla |
|---|---|---|
| 1 | Inmediato | Retry |
| 2 | 30 segundos | Retry |
| 3 | 5 minutos | Retry |
| 4 | — | Dead Letter Queue |

## Stack

- Java 21 + Spring Boot 3.x
- Spring Kafka (consumer)
- WebClient (HTTP async)
- Redis (tracking de entregas)
- PostgreSQL (webhooks registrados)
