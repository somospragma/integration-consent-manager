# API Consent Query — Documentación Funcional

## Propósito

Permite buscar, filtrar y paginar consentimientos. Los TPPs ven solo sus propios consentimientos. Los usuarios ven los consentimientos que han otorgado.

---

## GET /v1/consents — Listar con filtros

### ¿Qué hace?

Retorna una lista paginada de consentimientos del TPP autenticado, con filtros opcionales.

### Parámetros de query

| Parámetro | Tipo | Obligatorio | Descripción | Valores |
|---|---|---|---|---|
| `status` | string | No | Filtrar por estado | AWAITING_AUTHORIZATION, AUTHORIZED, REJECTED, REVOKED, EXPIRED, CONSUMED |
| `type` | string | No | Filtrar por tipo | ACCOUNTS, PAYMENTS, FUNDS_CONFIRMATION |
| `fromDate` | date | No | Desde fecha de creación | ISO 8601 (YYYY-MM-DD) |
| `toDate` | date | No | Hasta fecha de creación | ISO 8601 (YYYY-MM-DD) |
| `page` | integer | No | Número de página | Default: 1, mínimo: 1 |
| `pageSize` | integer | No | Registros por página | Default: 25, máximo: 100 |
| `sort` | string | No | Campo de ordenamiento | createdAt, updatedAt, expiresAt |
| `order` | string | No | Dirección | asc, desc (default: desc) |

### Response (200 OK)

| Campo | Tipo | Descripción |
|---|---|---|
| `data` | array | Lista de consentimientos |
| `data[].consentId` | string | ID del consentimiento |
| `data[].type` | string | Tipo |
| `data[].status` | string | Estado actual |
| `data[].tppId` | string | TPP que lo creó |
| `data[].tppName` | string | Nombre del TPP |
| `data[].permissions` | array | Permisos |
| `data[].createdAt` | datetime | Fecha de creación |
| `data[].expiresAt` | datetime | Fecha de expiración |
| `links.self` | string | URL de la página actual |
| `links.next` | string | URL de la siguiente página (null si es la última) |
| `links.prev` | string | URL de la página anterior (null si es la primera) |
| `meta.totalPages` | integer | Total de páginas |
| `meta.totalCount` | integer | Total de registros |
| `meta.currentPage` | integer | Página actual |
| `meta.pageSize` | integer | Tamaño de página |

### Reglas

- El TPP solo ve consentimientos que él creó (filtrado automático por token)
- Máximo 100 registros por página
- Ordenamiento por defecto: `createdAt` descendente (más recientes primero)

---

## GET /v1/consents/{consentId}/history — Historial de estados

### ¿Qué hace?

Retorna el historial completo de cambios de estado de un consentimiento. Útil para auditoría y debugging.

### Response

| Campo | Tipo | Descripción |
|---|---|---|
| `consentId` | string | ID del consentimiento |
| `events` | array | Lista de eventos ordenados cronológicamente |
| `events[].eventId` | string | ID único del evento |
| `events[].eventType` | string | Tipo de evento |
| `events[].previousStatus` | string | Estado anterior |
| `events[].newStatus` | string | Estado nuevo |
| `events[].actor` | string | Quién realizó la acción |
| `events[].actorType` | string | USER, TPP, SYSTEM o ADMIN |
| `events[].timestamp` | datetime | Cuándo ocurrió |

### Tipos de evento

| Evento | Descripción | Actor típico |
|---|---|---|
| `CREATED` | Consentimiento creado | TPP |
| `AWAITING_AUTHORIZATION` | Listo para autorizar | SYSTEM |
| `AUTHORIZED` | Usuario autorizó | USER |
| `REJECTED` | Usuario rechazó | USER |
| `REVOKED` | Consentimiento revocado | USER, TPP o ADMIN |
| `EXPIRED` | Expiró por TTL | SYSTEM |
| `CONSUMED` | Usado (pagos) | SYSTEM |

---

## GET /v1/users/{userId}/consents — Consentimientos de un usuario

### ¿Qué hace?

Retorna los consentimientos que un usuario ha otorgado. Usado para la interfaz de gestión del usuario (CMI - Consent Management Interface).

### Parámetros

| Parámetro | Ubicación | Tipo | Descripción |
|---|---|---|---|
| `userId` | path | string | ID del usuario |
| `status` | query | string | Filtrar por estado (opcional) |

### Response

Incluye información enriquecida para mostrar al usuario:

| Campo | Tipo | Descripción |
|---|---|---|
| `data[].consentId` | string | ID |
| `data[].tppName` | string | Nombre de la entidad que tiene acceso |
| `data[].tppLogo` | string (url) | Logo del TPP (para UI) |
| `data[].type` | string | Tipo de acceso |
| `data[].status` | string | Estado |
| `data[].permissionsDescription` | array[string] | Permisos en lenguaje humano |
| `data[].authorizedAt` | datetime | Cuándo autorizó |
| `data[].expiresAt` | datetime | Cuándo expira |
