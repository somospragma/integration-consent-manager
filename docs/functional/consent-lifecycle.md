# API Consent Lifecycle — Documentación Funcional

## Propósito

Permite a un TPP (entidad tercera) crear, consultar y revocar consentimientos de acceso a datos financieros del usuario.

---

## POST /v1/consents — Crear Consentimiento

### ¿Qué hace?

Crea un nuevo consentimiento en estado `AWAITING_AUTHORIZATION`. El TPP define qué tipo de datos necesita y qué permisos solicita. El consentimiento queda pendiente hasta que el usuario lo autorice.

### Request

| Campo | Tipo | Obligatorio | Descripción | Reglas |
|---|---|---|---|---|
| `data.type` | string | Sí | Tipo de consentimiento | Valores: `ACCOUNTS`, `PAYMENTS`, `FUNDS_CONFIRMATION` |
| `data.permissions` | array[string] | Sí | Permisos solicitados | Mínimo 1. Solo permisos válidos del catálogo |
| `data.expiresAt` | datetime | No | Fecha de expiración | ISO 8601. Debe ser futuro. Máximo 12 meses. Default: 365 días |
| `data.transactionFromDateTime` | datetime | No | Inicio del rango de transacciones | Solo para tipo ACCOUNTS |
| `data.transactionToDateTime` | datetime | No | Fin del rango de transacciones | Solo para tipo ACCOUNTS |

### Permisos válidos por tipo

| Tipo | Permisos permitidos |
|---|---|
| `ACCOUNTS` | READ_ACCOUNTS_BASIC, READ_ACCOUNTS_DETAIL, READ_BALANCES, READ_TRANSACTIONS_BASIC, READ_TRANSACTIONS_DETAIL, READ_BENEFICIARIES, READ_PRODUCTS, READ_STANDING_ORDERS |
| `PAYMENTS` | INITIATE_PAYMENT, READ_PAYMENT_STATUS |
| `FUNDS_CONFIRMATION` | CONFIRM_FUNDS |

### Response (201 Created)

| Campo | Tipo | Descripción |
|---|---|---|
| `data.consentId` | string (uuid) | Identificador único del consentimiento |
| `data.type` | string | Tipo solicitado |
| `data.status` | string | Siempre `AWAITING_AUTHORIZATION` al crear |
| `data.tppId` | string | ID del TPP que creó (extraído del token) |
| `data.permissions` | array[string] | Permisos otorgados |
| `data.expiresAt` | datetime | Fecha de expiración |
| `data.createdAt` | datetime | Fecha de creación |
| `data.updatedAt` | datetime | Última actualización |

### Ejemplo

```json
// Request
POST /v1/consents
{
  "data": {
    "type": "ACCOUNTS",
    "permissions": ["READ_ACCOUNTS_BASIC", "READ_BALANCES"],
    "expiresAt": "2027-06-01T00:00:00Z"
  }
}

// Response 201
{
  "data": {
    "consentId": "550e8400-e29b-41d4-a716-446655440000",
    "type": "ACCOUNTS",
    "status": "AWAITING_AUTHORIZATION",
    "tppId": "fintech-001",
    "permissions": ["READ_ACCOUNTS_BASIC", "READ_BALANCES"],
    "expiresAt": "2027-06-01T00:00:00Z",
    "createdAt": "2026-06-01T10:00:00Z",
    "updatedAt": "2026-06-01T10:00:00Z"
  }
}
```

### Errores posibles

| HTTP | Código | Causa |
|---|---|---|
| 400 | INVALID_REQUEST | Campos obligatorios faltantes |
| 400 | INVALID_PERMISSIONS | Permiso no existe o no aplica al tipo |
| 401 | UNAUTHORIZED | Token inválido o expirado |
| 429 | RATE_LIMIT_EXCEEDED | Más de 100 requests/min |

---

## GET /v1/consents/{consentId} — Consultar Consentimiento

### ¿Qué hace?

Retorna el detalle completo de un consentimiento. El TPP solo puede consultar consentimientos que él creó.

### Parámetros

| Parámetro | Ubicación | Tipo | Obligatorio | Descripción |
|---|---|---|---|---|
| `consentId` | path | string (uuid) | Sí | ID del consentimiento |

### Response (200 OK)

| Campo | Tipo | Descripción |
|---|---|---|
| `data.consentId` | string | ID del consentimiento |
| `data.type` | string | ACCOUNTS, PAYMENTS o FUNDS_CONFIRMATION |
| `data.status` | string | Estado actual (ver tabla de estados) |
| `data.tppId` | string | TPP que lo creó |
| `data.userId` | string | Usuario que autorizó (null si no autorizado) |
| `data.permissions` | array | Permisos otorgados |
| `data.expiresAt` | datetime | Cuándo expira |
| `data.authorizedAt` | datetime | Cuándo se autorizó (null si no) |
| `data.revokedAt` | datetime | Cuándo se revocó (null si no) |
| `data.createdAt` | datetime | Cuándo se creó |
| `data.updatedAt` | datetime | Última modificación |

### Estados posibles

| Estado | Significado | ¿Permite acceso a datos? |
|---|---|---|
| `AWAITING_AUTHORIZATION` | Creado, esperando al usuario | No |
| `AUTHORIZED` | Usuario aprobó | **Sí** |
| `REJECTED` | Usuario rechazó | No |
| `REVOKED` | Revocado por usuario, TPP o admin | No |
| `EXPIRED` | Pasó la fecha de expiración | No |
| `CONSUMED` | Usado (solo pagos, single-use) | No |

---

## DELETE /v1/consents/{consentId} — Revocar Consentimiento

### ¿Qué hace?

Revoca un consentimiento activo. Después de revocar, el TPP pierde acceso a los datos del usuario inmediatamente. Los tokens asociados se invalidan.

### Parámetros

| Parámetro | Ubicación | Tipo | Obligatorio | Descripción |
|---|---|---|---|---|
| `consentId` | path | string (uuid) | Sí | ID del consentimiento a revocar |

### Response

- **204 No Content** — Revocado exitosamente
- **404 Not Found** — Consentimiento no existe
- **409 Conflict** — No se puede revocar (ya está revocado/expirado)

### Reglas de negocio

- Solo se pueden revocar consentimientos en estado `AUTHORIZED` o `AWAITING_AUTHORIZATION`
- La revocación es inmediata e irreversible
- Todos los tokens asociados al consentimiento se invalidan
- Se envía webhook `consent.revoked` a los callbacks registrados
- Se registra en audit trail con actor = TPP

---

## Headers requeridos en todos los endpoints

| Header | Obligatorio | Formato | Descripción |
|---|---|---|---|
| `Authorization` | Sí | `Bearer {jwt}` | Token de acceso |
| `X-Fapi-Interaction-Id` | Sí | UUID v4 | ID de correlación para trazabilidad |
| `X-Fapi-Auth-Date` | Sí | RFC 7231 date | Fecha de autenticación del usuario |
| `X-Idempotency-Key` | Solo POST | UUID v4 | Previene duplicados |
| `Content-Type` | Solo POST | `application/json` | Tipo de contenido |
