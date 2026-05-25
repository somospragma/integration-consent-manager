# MS-audit-trail

## Descripción

Microservicio de auditoría inmutable. Registra cada operación sobre consentimientos con hash encadenado para garantizar integridad y no-repudio.

## Responsabilidad

- Registrar eventos de auditoría (append-only)
- Hash encadenado (cada registro incluye hash del anterior)
- Consulta de logs por consentimiento, actor, fecha
- Exportación para regulador
- Enmascaramiento de datos sensibles
- Retención mínima 5 años

## Eventos Consumidos (Kafka)

| Topic | Evento | Acción |
|---|---|---|
| `consent-events` | `consent.*` | Registrar en audit log |
| `api-access-events` | `api.request` | Registrar acceso a datos |

## Endpoints Internos

| Método | Endpoint | Descripción |
|---|---|---|
| POST | `/v1/audit/log` | Registrar entrada (sync) |
| GET | `/v1/audit?consentId={id}` | Logs de un consent |
| GET | `/v1/audit?actorId={id}` | Logs de un actor |
| GET | `/v1/audit/export` | Exportar para regulador |
| GET | `/v1/audit/integrity-check` | Verificar cadena de hashes |

## Modelo de Datos

```json
{
  "auditId": "uuid",
  "timestamp": "2026-06-01T10:00:00Z",
  "consentId": "uuid",
  "action": "AUTHORIZE",
  "actor": "user-12345",
  "actorType": "USER",
  "previousState": "AWAITING_AUTHORIZATION",
  "newState": "AUTHORIZED",
  "ipAddress": "104.25.xxx.xxx",
  "metadata": {},
  "hash": "sha256-of-this-record",
  "previousHash": "sha256-of-previous-record"
}
```

## Stack

- Java 21 + Spring Boot 3.x
- PostgreSQL (append-only table, partitioned by month)
- Spring Kafka (consumer)
- Scheduled job para verificación de integridad

## Requisitos No Funcionales

- Tabla append-only (no UPDATE, no DELETE)
- Particionamiento por mes
- Retención: 5 años mínimo
- Archivado a Object Storage después de 1 año
- Campos sensibles enmascarados (IP, user data)
