# API Consent Admin — Documentación Funcional

## Propósito

API para administradores y oficiales de la entidad financiera. Permite búsqueda sin restricciones, revocación administrativa, operaciones masivas y monitoreo.

**Acceso:** Solo red interna. Requiere rol ADMIN o CUSTOMER_CARE_OFFICER.

---

## GET /v1/admin/consents/search — Búsqueda avanzada

### ¿Qué hace?

Busca consentimientos sin restricción de TPP o usuario. Un admin puede ver todos los consentimientos del sistema.

### Parámetros

| Parámetro | Tipo | Descripción |
|---|---|---|
| `consentId` | string | Buscar por ID exacto |
| `userId` | string | Filtrar por usuario |
| `tppId` | string | Filtrar por TPP |
| `status` | string | Filtrar por estado |
| `type` | string | Filtrar por tipo |
| `fromDate` | date | Desde fecha |
| `toDate` | date | Hasta fecha |
| `page` | integer | Página (default: 1) |
| `pageSize` | integer | Tamaño (default: 50, max: 200) |

---

## DELETE /v1/admin/consents/{consentId} — Revocación administrativa

### ¿Qué hace?

Revoca un consentimiento por decisión administrativa. Requiere motivo obligatorio.

### Request

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `reason` | string | Sí | Motivo de la revocación |
| `notes` | string | No | Notas adicionales |

### Motivos válidos

| Código | Cuándo usar |
|---|---|
| `TPP_SUSPENDED` | El TPP fue suspendido del directorio |
| `SECURITY_INCIDENT` | Incidente de seguridad detectado |
| `REGULATORY_ORDER` | Orden del regulador |
| `ADMIN_DECISION` | Decisión administrativa |

---

## POST /v1/admin/consents/bulk-revoke — Revocación masiva

### ¿Qué hace?

Revoca múltiples consentimientos que cumplan un criterio. Ejemplo: revocar todos los consents de un TPP suspendido.

### Request

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `criteria.tppId` | string | No | Revocar todos los de este TPP |
| `criteria.status` | string | No | Solo en este estado |
| `criteria.createdBefore` | datetime | No | Creados antes de esta fecha |
| `reason` | string | Sí | Motivo |
| `dryRun` | boolean | No | Default: true. Si true, solo cuenta cuántos se afectarían |

### Response

| Campo | Tipo | Descripción |
|---|---|---|
| `jobId` | string | ID del job (operación async) |
| `affectedCount` | integer | Cuántos consentimientos se afectan |
| `status` | string | ACCEPTED o DRY_RUN_COMPLETE |

**Importante:** Siempre ejecutar primero con `dryRun: true` para verificar el impacto.

---

## GET /v1/admin/metrics — Métricas

### Response

| Campo | Tipo | Descripción |
|---|---|---|
| `totalConsents` | integer | Total de consentimientos en el sistema |
| `activeConsents` | integer | Consentimientos en estado AUTHORIZED |
| `consentsByStatus` | object | Conteo por estado |
| `consentsByType` | object | Conteo por tipo |
| `authorizationRate` | float | % de consents que se autorizan |
| `avgLifetimeDays` | float | Vida promedio de un consent |
| `topTpps` | array | TPPs con más consentimientos |

---

## GET /v1/admin/config — Configuración

### Response

| Campo | Tipo | Descripción |
|---|---|---|
| `ttl.accountsDays` | integer | TTL para consents de cuentas (default: 365) |
| `ttl.paymentsHours` | integer | TTL para consents de pagos (default: 24) |
| `ttl.fundsConfirmationHours` | integer | TTL para funds (default: 24) |
| `rateLimits.perTppPerMinute` | integer | Rate limit por TPP |
| `rateLimits.globalPerMinute` | integer | Rate limit global |
| `features.webhooksEnabled` | boolean | Si webhooks están activos |
| `features.bulkRevokeEnabled` | boolean | Si bulk revoke está activo |

## PUT /v1/admin/config — Actualizar configuración

Permite cambiar TTLs y rate limits en caliente sin redeploy.

---

## GET /v1/admin/health — Health check

### Response

| Campo | Tipo | Descripción |
|---|---|---|
| `status` | string | UP, DOWN o DEGRADED |
| `components.database` | string | Estado de PostgreSQL |
| `components.cache` | string | Estado de Redis |
| `components.kafka` | string | Estado de Kafka |
| `timestamp` | datetime | Hora del check |
