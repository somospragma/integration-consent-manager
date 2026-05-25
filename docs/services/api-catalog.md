# Catálogo de APIs — Consent Manager

## Alineación con Estándares

| Estándar | Aplicación en Consent Manager |
|---|---|
| **BIAN v11** | Nomenclatura de Service Domains, operaciones CRUD estándar (Initiate, Update, Retrieve, Execute) |
| **ISO 20022** | Modelo de datos para pagos (pain.001), identificadores, códigos de estado |
| **FAPI 2.0** | Seguridad: mTLS, PAR, PKCE, certificate binding, JWT firmado |
| **Open Banking UK v4.0** | Estructura de endpoints, permisos, flujo de consentimiento |

## Mapeo BIAN Service Domains

| BIAN Service Domain | Componente Consent Manager | Descripción BIAN |
|---|---|---|
| **Customer Access Entitlement** | API-consent-lifecycle | Gestión de permisos de acceso del cliente a productos/servicios |
| **Party Authentication** | API-consent-authorization | Confirmación de identidad del cliente en canales interactivos |
| **Party Data Management** | MS-permission-registry | Gestión de datos y preferencias del cliente |
| **Customer Products and Services** | MS-consent-engine | Detalle de productos/servicios que el cliente ha adquirido |
| **Regulatory Compliance** | MS-audit-trail | Cumplimiento regulatorio y trazabilidad |

---

## Catálogo Completo de APIs

### 1. API-consent-lifecycle

**BIAN Domain:** Customer Access Entitlement
**Propósito:** Gestión del ciclo de vida del consentimiento (crear, consultar, revocar)

| # | Operación BIAN | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 1 | Initiate | POST | `/v1/consents` | Crear nuevo consentimiento |
| 2 | Retrieve | GET | `/v1/consents/{consentId}` | Obtener detalle |
| 3 | Retrieve | GET | `/v1/consents/{consentId}/status` | Solo estado actual |
| 4 | Update | PATCH | `/v1/consents/{consentId}` | Actualizar metadata |
| 5 | Execute (Revoke) | DELETE | `/v1/consents/{consentId}` | Revocar consentimiento |

**Seguridad requerida:**

```yaml
security:
  transport:
    - tls: "1.3"
    - mtls: required
    - certificate_validation: [CA, expiry, CRL/OCSP]
  authentication:
    - method: "Bearer JWT"
    - token_binding: "cnf.x5t#S256"
    - grant_type: "client_credentials"
  authorization:
    - scope_required: "consents"
    - tpp_validation: "directory_registered"
  headers_required:
    - "Authorization: Bearer {jwt}"
    - "X-Fapi-Interaction-Id: {uuid}"
    - "X-Fapi-Auth-Date: {iso8601}"
    - "X-Idempotency-Key: {uuid}" # solo POST
  rate_limiting:
    - per_tpp: "100 req/min"
    - burst: "20 req/sec"
  input_validation:
    - type: "enum [ACCOUNTS, PAYMENTS, FUNDS_CONFIRMATION]"
    - permissions: "array of valid permission codes"
    - expiresAt: "ISO 8601, future date, max 12 months"
```

---

### 2. API-consent-authorization

**BIAN Domain:** Party Authentication
**Propósito:** Integración con Authorization Server para flujo FAPI 2.0

| # | Operación BIAN | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 6 | Retrieve | GET | `/v1/consents/{consentId}/validate` | Validar para pantalla de auth |
| 7 | Execute (Authorize) | POST | `/v1/consents/{consentId}/authorize` | Usuario autorizó (post-SCA) |
| 8 | Execute (Reject) | POST | `/v1/consents/{consentId}/reject` | Usuario rechazó |
| 9 | Retrieve | GET | `/v1/consents/{consentId}/active` | Verificar vigencia (gateway) |
| 10 | Execute (Consume) | POST | `/v1/consents/{consentId}/consume` | Marcar como consumido |

**Seguridad requerida:**

```yaml
security:
  transport:
    - mtls: "pod-to-pod (Istio STRICT)"
  authentication:
    - method: "internal service mesh identity"
    - no_external_access: true
  authorization:
    - caller_must_be: ["authorization-server", "api-gateway"]
    - service_account_validation: true
  input_validation:
    - userId: "non-empty string, max 100 chars"
    - accountIds: "array of valid account identifiers"
    - authenticationMethod: "enum [SCA_BIOMETRIC, SCA_OTP, SCA_PUSH]"
    - reason: "enum [USER_REJECTED, TIMEOUT, SYSTEM_ERROR]"
```

---

### 3. API-consent-query

**BIAN Domain:** Customer Access Entitlement (Retrieve operations)
**Propósito:** Búsqueda, filtrado y paginación de consentimientos

| # | Operación BIAN | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 11 | Retrieve (List) | GET | `/v1/consents` | Listar con filtros |
| 12 | Retrieve (History) | GET | `/v1/consents/{consentId}/history` | Historial de estados |
| 13 | Retrieve (By User) | GET | `/v1/users/{userId}/consents` | Consents de un usuario |
| 14 | Retrieve (By TPP) | GET | `/v1/tpp/{tppId}/consents` | Consents de un TPP |

**Seguridad requerida:**

```yaml
security:
  transport:
    - mtls: required
  authentication:
    - method: "Bearer JWT"
    - token_binding: "cnf.x5t#S256"
  authorization:
    - scope_required: "consents"
    - data_isolation: "TPP only sees own consents"
    - user_isolation: "User only sees own consents"
  pagination:
    - max_page_size: 100
    - default_page_size: 25
  rate_limiting:
    - per_tpp: "200 req/min"
```

---

### 4. API-consent-admin

**BIAN Domain:** Regulatory Compliance + Customer Access Entitlement
**Propósito:** Administración interna (operadores, oficiales)

| # | Operación BIAN | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 15 | Retrieve (Search) | GET | `/v1/admin/consents/search` | Búsqueda avanzada |
| 16 | Execute (Revoke) | DELETE | `/v1/admin/consents/{consentId}` | Revocación administrativa |
| 17 | Execute (Bulk) | POST | `/v1/admin/consents/bulk-revoke` | Revocación masiva |
| 18 | Retrieve (Metrics) | GET | `/v1/admin/metrics` | Métricas operativas |
| 19 | Retrieve (Config) | GET | `/v1/admin/config` | Configuración actual |
| 20 | Update (Config) | PUT | `/v1/admin/config` | Actualizar configuración |
| 21 | Retrieve (Health) | GET | `/v1/admin/health` | Health check |
| 22 | Retrieve (Ready) | GET | `/v1/admin/health/ready` | Readiness probe |
| 23 | Retrieve (Live) | GET | `/v1/admin/health/live` | Liveness probe |

**Seguridad requerida:**

```yaml
security:
  transport:
    - internal_only: true
    - mtls: "pod-to-pod"
  authentication:
    - method: "Bearer JWT with admin role"
    - roles_required: ["ADMIN", "CUSTOMER_CARE_OFFICER"]
  authorization:
    - rbac: true
    - audit_all_operations: true
    - bulk_operations_require: "ADMIN role + reason"
  input_validation:
    - reason: "required for revoke operations"
    - dryRun: "default true for bulk operations"
```

---

### 5. API-webhook-management

**BIAN Domain:** Party Data Management (Notifications)
**Propósito:** Gestión de webhooks para notificaciones a TPPs

| # | Operación BIAN | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 24 | Initiate | POST | `/v1/webhooks` | Registrar webhook |
| 25 | Retrieve (List) | GET | `/v1/webhooks` | Listar webhooks del TPP |
| 26 | Retrieve | GET | `/v1/webhooks/{webhookId}` | Detalle de un webhook |
| 27 | Execute (Delete) | DELETE | `/v1/webhooks/{webhookId}` | Eliminar webhook |
| 28 | Retrieve (Deliveries) | GET | `/v1/webhooks/{webhookId}/deliveries` | Historial de entregas |
| 29 | Execute (Test) | POST | `/v1/webhooks/{webhookId}/test` | Enviar test payload |

**Seguridad requerida:**

```yaml
security:
  transport:
    - mtls: required
  authentication:
    - method: "Bearer JWT"
    - scope_required: "webhooks"
  authorization:
    - tpp_isolation: "TPP only manages own webhooks"
  input_validation:
    - url: "HTTPS only, no localhost, no private IPs"
    - eventTypes: "array of valid event types"
  webhook_security:
    - signature: "HMAC-SHA256 on payload"
    - header: "X-Webhook-Signature: sha256={hmac}"
    - timestamp: "X-Webhook-Timestamp for replay prevention"
    - secret_rotation: "supported via PUT /webhooks/{id}/rotate-secret"
```

---

### 6. API-permission-catalog

**BIAN Domain:** Customer Products and Services
**Propósito:** Catálogo de permisos y validación de acceso

| # | Operación BIAN | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 30 | Retrieve (Catalog) | GET | `/v1/permissions` | Catálogo completo |
| 31 | Retrieve (By Type) | GET | `/v1/permissions?type={consentType}` | Permisos por tipo |
| 32 | Execute (Validate) | GET | `/v1/permissions/validate` | Validar permiso para endpoint |
| 33 | Retrieve (Purposes) | GET | `/v1/purposes` | Catálogo de propósitos |
| 34 | Initiate (Purpose) | POST | `/v1/purposes` | Crear propósito (admin) |

**Seguridad requerida:**

```yaml
security:
  transport:
    - internal_only: true (catalog)
    - mtls: required (validate endpoint used by gateway)
  authentication:
    - catalog: "public read (no auth for GET /permissions)"
    - validate: "internal service identity"
    - create_purpose: "admin role required"
  caching:
    - catalog: "cache 1 hour (rarely changes)"
    - validate: "cache 5 min (hot path)"
```

---

### 7. API-audit

**BIAN Domain:** Regulatory Compliance
**Propósito:** Consulta de logs de auditoría

| # | Operación BIAN | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 35 | Retrieve (By Consent) | GET | `/v1/audit?consentId={id}` | Logs de un consent |
| 36 | Retrieve (By Actor) | GET | `/v1/audit?actorId={id}` | Logs de un actor |
| 37 | Retrieve (By Date) | GET | `/v1/audit?from={d}&to={d}` | Logs por rango |
| 38 | Retrieve (Export) | GET | `/v1/audit/export` | Exportar para regulador |
| 39 | Execute (Integrity) | GET | `/v1/audit/integrity-check` | Verificar cadena de hashes |

**Seguridad requerida:**

```yaml
security:
  transport:
    - internal_only: true
  authentication:
    - roles_required: ["ADMIN", "AUDITOR", "REGULATOR"]
  authorization:
    - export_requires: "AUDITOR or REGULATOR role"
    - integrity_check: "ADMIN only"
  data_protection:
    - pii_masking: "IP addresses masked"
    - sensitive_fields: "encrypted at rest"
    - retention: "5 years minimum (regulatory)"
```

---

### 8. API-consent-flow (Orquestador)

**BIAN Domain:** Customer Access Entitlement (Orchestrated)
**Propósito:** Orquestación del flujo completo de consentimiento de cuentas

| # | Operación BIAN | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 40 | Initiate | POST | `/v1/consent-flows` | Iniciar flujo |
| 41 | Execute (Authorize) | POST | `/v1/consent-flows/{flowId}/authorize` | Usuario autorizó |
| 42 | Execute (Reject) | POST | `/v1/consent-flows/{flowId}/reject` | Usuario rechazó |
| 43 | Execute (Token) | POST | `/v1/consent-flows/{flowId}/token` | Intercambiar code |
| 44 | Retrieve (Status) | GET | `/v1/consent-flows/{flowId}` | Estado del flujo |

---

### 9. API-payment-consent-flow (Orquestador)

**BIAN Domain:** Payment Initiation + Customer Access Entitlement
**Propósito:** Orquestación del flujo de consentimiento de pagos

| # | Operación BIAN | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 45 | Initiate | POST | `/v1/payment-consent-flows` | Iniciar flujo de pago |
| 46 | Execute (Authorize) | POST | `/v1/payment-consent-flows/{flowId}/authorize` | Usuario autorizó pago |
| 47 | Execute (Funds) | POST | `/v1/payment-consent-flows/{flowId}/check-funds` | Verificar fondos |
| 48 | Execute (Pay) | POST | `/v1/payment-consent-flows/{flowId}/execute` | Ejecutar pago |
| 49 | Retrieve (Status) | GET | `/v1/payment-consent-flows/{flowId}` | Estado del flujo/pago |

**Seguridad adicional para pagos (ISO 20022 aligned):**

```yaml
security:
  sca:
    - always_required: true  # Sin excepciones para pagos
    - factors: "minimum 2 (knowledge + possession/inherence)"
  payment_validation:
    - amount: "positive decimal, max 2 decimals"
    - currency: "ISO 4217 (COP, USD, EUR)"
    - creditor_account: "valid format per scheme"
    - remittance_reference: "max 140 chars (ISO 20022 pain.001)"
  idempotency:
    - required: true
    - header: "X-Idempotency-Key"
    - ttl: "24 hours"
  consent_type:
    - single_use: true  # Se consume después del pago
```

---

## Resumen del Catálogo

| API | Endpoints | Acceso | BIAN Domain |
|---|---|---|---|
| API-consent-lifecycle | 5 | Externo (TPPs) | Customer Access Entitlement |
| API-consent-authorization | 5 | Interno (Auth Server, Gateway) | Party Authentication |
| API-consent-query | 4 | Externo (TPPs, Users) | Customer Access Entitlement |
| API-consent-admin | 9 | Interno (Admins) | Regulatory Compliance |
| API-webhook-management | 6 | Externo (TPPs) | Party Data Management |
| API-permission-catalog | 5 | Mixto (público + interno) | Customer Products and Services |
| API-audit | 5 | Interno (Admins, Regulador) | Regulatory Compliance |
| API-consent-flow | 5 | Interno (Orquestación) | Customer Access Entitlement |
| API-payment-consent-flow | 5 | Interno (Orquestación) | Payment Initiation |
| **TOTAL** | **49 endpoints** | | |

---

## Validaciones de Seguridad Transversales (Todas las APIs)

### Capa de Transporte

| Control | Valor | Estándar |
|---|---|---|
| TLS Version | 1.3 (mínimo 1.2) | FAPI 2.0 |
| mTLS | Obligatorio para APIs externas | FAPI 2.0 |
| Certificate Validation | CA + Expiry + CRL/OCSP | X.509 |
| Cipher Suites | Solo Forward Secrecy (ECDHE) | NIST |

### Capa de Autenticación

| Control | Valor | Estándar |
|---|---|---|
| Token Format | JWT (at+jwt) | RFC 9068 |
| Signing Algorithm | PS256 o ES256 | FAPI 2.0 |
| Token Binding | cnf.x5t#S256 (cert hash) | RFC 8705 |
| Client Auth | private_key_jwt | OIDC Core |
| Token Lifetime | 15 min (access), 90 días (refresh) | FAPI 2.0 |
| PAR | Obligatorio | RFC 9126 |
| PKCE | S256 obligatorio | RFC 7636 |

### Capa de Autorización

| Control | Valor | Estándar |
|---|---|---|
| Consent Enforcement | Cada request valida consent activo | Open Banking UK |
| Scope Validation | Token scope ⊇ endpoint requirement | OAuth 2.0 |
| Permission Check | Consent permissions ⊇ resource | BIAN |
| Data Isolation | TPP solo ve sus datos | GDPR/Ley 1581 |
| Time-bound | Consent TTL enforced | Open Banking UK |

### Capa de Datos (ISO 20022)

| Campo | Validación | Referencia ISO 20022 |
|---|---|---|
| Amount | Decimal positivo, max 2 decimales | ActiveCurrencyAndAmount |
| Currency | ISO 4217 (3 chars) | CurrencyCode |
| Account ID | Formato según scheme | AccountIdentification |
| Remittance Reference | Max 140 chars | RemittanceInformation |
| Transaction ID | UUID o formato banco | EndToEndIdentification |
| Timestamp | ISO 8601 UTC | ISODateTime |
| Country | ISO 3166-1 alpha-3 | CountryCode |

### Headers FAPI Obligatorios

| Header | Dirección | Obligatorio | Descripción |
|---|---|---|---|
| `Authorization` | Request | Sí | Bearer JWT |
| `X-Fapi-Interaction-Id` | Request/Response | Sí | UUID correlación |
| `X-Fapi-Auth-Date` | Request | Sí | Fecha auth del usuario |
| `X-Fapi-Customer-Ip-Address` | Request | Condicional | IP del usuario final |
| `X-Idempotency-Key` | Request (POST) | Sí | Idempotencia |
| `X-Fapi-Financial-Id` | Request | Sí | ID de la entidad |
| `Strict-Transport-Security` | Response | Sí | HSTS |
| `X-Content-Type-Options` | Response | Sí | nosniff |
| `Cache-Control` | Response | Sí | no-store |
| `X-RateLimit-Limit` | Response | Sí | Límite de rate |
| `X-RateLimit-Remaining` | Response | Sí | Requests restantes |

---

## Códigos de Error Estandarizados

| HTTP Code | Error Code | Descripción | Cuándo |
|---|---|---|---|
| 400 | `INVALID_REQUEST` | Request malformado | Validación de input falla |
| 400 | `INVALID_PERMISSIONS` | Permisos no válidos | Permiso no existe en catálogo |
| 401 | `UNAUTHORIZED` | No autenticado | Token inválido/expirado/ausente |
| 403 | `FORBIDDEN` | Sin permisos | Scope insuficiente |
| 403 | `CONSENT_NOT_ACTIVE` | Consent no activo | Consent revocado/expirado |
| 403 | `INSUFFICIENT_PERMISSIONS` | Permiso faltante | Consent no tiene el permiso |
| 404 | `CONSENT_NOT_FOUND` | No encontrado | ID no existe |
| 409 | `INVALID_STATE_TRANSITION` | Estado inválido | Transición no permitida |
| 429 | `RATE_LIMIT_EXCEEDED` | Rate limit | Demasiados requests |
| 500 | `INTERNAL_ERROR` | Error interno | Error no esperado |

**Formato de error (ISO 20022 inspired):**

```json
{
  "code": "INVALID_STATE_TRANSITION",
  "message": "Consent cannot transition from REVOKED to AUTHORIZED",
  "errors": [
    {
      "field": "status",
      "code": "UK.OBIE.Field.Invalid",
      "message": "Current state does not allow this operation"
    }
  ],
  "interactionId": "93bac548-d2de-4546-b106-880a5018460d",
  "timestamp": "2026-06-01T10:00:00.000Z"
}
```
