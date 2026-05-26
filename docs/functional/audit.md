# API Audit — Documentación Funcional

## Propósito

Consulta de logs de auditoría inmutables. Cada operación sobre consentimientos queda registrada con hash encadenado para garantizar integridad y no-repudio. Retención mínima: 5 años.

**Acceso:** Solo red interna. Requiere rol ADMIN, AUDITOR o REGULATOR.

---

## GET /v1/audit — Consultar logs

### Parámetros (al menos uno requerido)

| Parámetro | Tipo | Descripción |
|---|---|---|
| `consentId` | string (uuid) | Logs de un consentimiento específico |
| `actorId` | string | Logs de un actor (usuario, TPP, admin) |
| `action` | string | Filtrar por acción |
| `from` | datetime | Desde fecha |
| `to` | datetime | Hasta fecha |
| `page` | integer | Página (default: 1) |
| `pageSize` | integer | Tamaño (default: 50, max: 100) |

### Acciones registradas

| Acción | Descripción |
|---|---|
| `CREATE` | Consentimiento creado |
| `AUTHORIZE` | Usuario autorizó |
| `REJECT` | Usuario rechazó |
| `REVOKE` | Consentimiento revocado |
| `EXPIRE` | Expirado por sistema |
| `CONSUME` | Consumido (pagos) |

### Response

| Campo | Tipo | Descripción |
|---|---|---|
| `data[].auditId` | string (uuid) | ID único del registro |
| `data[].consentId` | string (uuid) | Consentimiento afectado |
| `data[].action` | string | Acción realizada |
| `data[].actorId` | string | Quién la realizó |
| `data[].actorType` | string | USER, TPP, SYSTEM o ADMIN |
| `data[].previousState` | string | Estado antes de la acción |
| `data[].newState` | string | Estado después de la acción |
| `data[].ipAddress` | string | IP de origen (último octeto enmascarado) |
| `data[].hash` | string | SHA-256 de este registro |
| `data[].previousHash` | string | SHA-256 del registro anterior (cadena) |
| `data[].createdAt` | datetime | Timestamp exacto |

### Integridad (hash encadenado)

Cada registro incluye:
- `hash`: SHA-256 del contenido del registro actual
- `previousHash`: hash del registro inmediatamente anterior

Si alguien modifica un registro, la cadena se rompe y se detecta en la verificación de integridad.

---

## GET /v1/audit/export — Exportar para regulador

### ¿Qué hace?

Genera un archivo exportable con los logs de un período. Útil para entregar al regulador o auditor externo.

### Parámetros

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `from` | datetime | Sí | Inicio del período |
| `to` | datetime | Sí | Fin del período |
| `format` | string | No | json o csv (default: json) |

### Response

| Campo | Tipo | Descripción |
|---|---|---|
| `downloadUrl` | string (uri) | URL presignada para descargar el archivo |
| `recordCount` | integer | Cantidad de registros exportados |
| `generatedAt` | datetime | Cuándo se generó |

---

## GET /v1/audit/integrity-check — Verificar integridad

### ¿Qué hace?

Recorre la cadena de hashes y verifica que ningún registro ha sido manipulado.

### Parámetros

| Parámetro | Tipo | Descripción |
|---|---|---|
| `sampleSize` | integer | Cantidad de registros a verificar (default: 1000) |

### Response

| Campo | Tipo | Descripción |
|---|---|---|
| `valid` | boolean | true si la cadena es íntegra |
| `sampleSize` | integer | Registros verificados |
| `checkedAt` | datetime | Cuándo se verificó |
| `firstViolationAt` | string | ID del primer registro con violación (null si todo OK) |
