# API Authorization Server — Documentación Funcional

## Propósito

Emite tokens de acceso JWT bajo el perfil FAPI 2.0. Gestiona el flujo OAuth 2.0 con PAR obligatorio, PKCE y certificate binding.

---

## GET /.well-known/openid-configuration — Discovery

### ¿Qué hace?

Retorna la configuración del servidor para que los clientes se auto-configuren. No requiere autenticación.

### Campos principales de la respuesta

| Campo | Descripción |
|---|---|
| `issuer` | URL del emisor de tokens |
| `authorization_endpoint` | URL para iniciar autorización |
| `token_endpoint` | URL para obtener tokens |
| `pushed_authorization_request_endpoint` | URL para PAR |
| `jwks_uri` | URL de claves públicas |
| `scopes_supported` | Scopes disponibles |
| `grant_types_supported` | Grant types soportados |
| `token_endpoint_auth_methods_supported` | Métodos de autenticación de cliente |
| `require_pushed_authorization_requests` | true (PAR obligatorio) |
| `tls_client_certificate_bound_access_tokens` | true (certificate binding) |

---

## POST /par — Pushed Authorization Request

### ¿Qué hace?

El TPP envía los parámetros de autorización directamente al servidor (no en la URL del browser). Retorna un `request_uri` de uso único con TTL de 60 segundos.

### Request (form-urlencoded)

| Campo | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `response_type` | string | Sí | Siempre `code` |
| `client_id` | string | Sí | ID del TPP |
| `redirect_uri` | string | Sí | URL de callback (debe coincidir con la registrada) |
| `scope` | string | Sí | Scopes solicitados (ej: `openid accounts`) |
| `code_challenge` | string | Sí | PKCE challenge (SHA-256 del verifier) |
| `code_challenge_method` | string | Sí | Siempre `S256` |
| `client_assertion_type` | string | Sí | `urn:ietf:params:oauth:client-assertion-type:jwt-bearer` |
| `client_assertion` | string | Sí | JWT firmado por el TPP |
| `claims` | string | No | JSON con consent_id |
| `state` | string | No | Estado para CSRF protection |

### Response (201 Created)

| Campo | Tipo | Descripción |
|---|---|---|
| `request_uri` | string | URI de uso único para /authorize |
| `expires_in` | integer | Segundos de validez (60) |

---

## POST /token — Obtener tokens

### ¿Qué hace?

Emite access_token, refresh_token e id_token. Soporta 3 grant types.

### Grant Type: authorization_code

| Campo | Obligatorio | Descripción |
|---|---|---|
| `grant_type` | Sí | `authorization_code` |
| `code` | Sí | Código recibido en el callback |
| `redirect_uri` | Sí | Misma URI del PAR |
| `code_verifier` | Sí | PKCE verifier original |
| `client_assertion_type` | Sí | JWT bearer |
| `client_assertion` | Sí | JWT firmado |

### Grant Type: client_credentials

| Campo | Obligatorio | Descripción |
|---|---|---|
| `grant_type` | Sí | `client_credentials` |
| `scope` | Sí | Scopes solicitados (ej: `consents`) |
| `client_assertion_type` | Sí | JWT bearer |
| `client_assertion` | Sí | JWT firmado |

### Grant Type: refresh_token

| Campo | Obligatorio | Descripción |
|---|---|---|
| `grant_type` | Sí | `refresh_token` |
| `refresh_token` | Sí | Refresh token actual |
| `client_assertion_type` | Sí | JWT bearer |
| `client_assertion` | Sí | JWT firmado |

### Response

| Campo | Tipo | Descripción |
|---|---|---|
| `access_token` | string | JWT firmado con certificate binding |
| `token_type` | string | `Bearer` |
| `expires_in` | integer | Segundos de validez (900 = 15 min) |
| `refresh_token` | string | Token para renovar (rotación: nuevo en cada uso) |
| `scope` | string | Scopes otorgados |
| `id_token` | string | JWT con identidad del usuario |

### Contenido del access_token (JWT)

| Claim | Descripción |
|---|---|
| `iss` | Emisor (URL del auth server) |
| `sub` | ID del usuario |
| `aud` | Audiencia (URL de la API) |
| `client_id` | ID del TPP |
| `scope` | Scopes otorgados |
| `consent_id` | ID del consentimiento vinculado |
| `exp` | Expiración (15 min) |
| `cnf.x5t#S256` | Hash del certificado mTLS del TPP (binding) |

---

## POST /revoke — Revocar token

Invalida un access_token o refresh_token. Siempre retorna 200 (no revela si el token existía).

## POST /introspect — Introspección

Retorna si un token está activo y sus claims. Solo accesible internamente (API Gateway).

## GET /userinfo — Info del usuario

Retorna datos del usuario autenticado (sub, name, email). Requiere access_token con scope `openid`.
