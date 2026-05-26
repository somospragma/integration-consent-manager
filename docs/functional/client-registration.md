# API Client Registration (DCR) — Documentación Funcional

## Propósito

Permite a TPPs registrarse automáticamente en el ecosistema presentando un Software Statement Assertion (SSA) firmado por el directorio central (Raidiam).

---

## POST /register — Registrar TPP

### ¿Qué hace?

Crea un nuevo client_id para el TPP. El TPP presenta su SSA (JWT firmado por el directorio) y recibe credenciales para operar.

### Request

| Campo | Tipo | Obligatorio | Descripción | Reglas |
|---|---|---|---|---|
| `software_statement` | string (JWT) | Sí | SSA firmado por el directorio | Se valida firma con JWKS del directorio |
| `redirect_uris` | array[string] | Sí | URLs de callback | Deben ser HTTPS. Deben coincidir con las del directorio |
| `token_endpoint_auth_method` | string | No | Método de autenticación | `private_key_jwt` (default) o `tls_client_auth` |
| `grant_types` | array[string] | No | Grant types solicitados | Default: authorization_code, client_credentials, refresh_token |
| `response_types` | array[string] | No | Response types | Solo `code` permitido |
| `scope` | string | No | Scopes solicitados | Deben estar dentro de los permitidos por el rol del TPP |
| `id_token_signed_response_alg` | string | No | Algoritmo para ID token | PS256 o ES256 |
| `request_object_signing_alg` | string | No | Algoritmo para request objects | PS256 o ES256 |

### Validaciones

1. **SSA válido** — Firma verificada con JWKS del directorio
2. **SSA no expirado** — Claim `exp` vigente
3. **Software activo** — El software_id del SSA está activo en el directorio
4. **Roles válidos** — El TPP tiene los roles necesarios (AISP, PISP)
5. **Redirect URIs** — Coinciden con las registradas en el directorio
6. **No duplicado** — El software_id no está ya registrado

### Response (201 Created)

| Campo | Tipo | Descripción |
|---|---|---|
| `client_id` | string | ID asignado al TPP (usar en todos los requests) |
| `client_id_issued_at` | integer | Unix timestamp de emisión |
| `token_endpoint_auth_method` | string | Método de auth configurado |
| `grant_types` | array | Grant types habilitados |
| `redirect_uris` | array | URIs registradas |
| `scope` | string | Scopes permitidos |
| `software_id` | string | ID del software en el directorio |
| `registration_access_token` | string | Token para gestionar este registro (GET/PUT/DELETE) |

---

## GET /register/{clientId} — Obtener configuración

### ¿Qué hace?

Retorna la configuración actual del cliente registrado.

**Autenticación:** Requiere el `registration_access_token` recibido al registrarse.

---

## PUT /register/{clientId} — Actualizar configuración

### ¿Qué hace?

Actualiza redirect_uris, scopes u otros parámetros del registro.

### Campos actualizables

| Campo | Descripción |
|---|---|
| `redirect_uris` | Cambiar URLs de callback |
| `scope` | Cambiar scopes solicitados |
| `token_endpoint_auth_method` | Cambiar método de auth |

---

## DELETE /register/{clientId} — Eliminar registro

### ¿Qué hace?

Elimina el registro del TPP. Todos los tokens y consentimientos asociados se invalidan.

**Irreversible.** El TPP deberá registrarse de nuevo para operar.
