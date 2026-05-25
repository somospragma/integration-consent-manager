# API-authorization-server

**BIAN Domain:** Party Authentication

## Descripción

API del Authorization Server FAPI 2.0. Emite tokens JWT sender-constrained, gestiona flujos OAuth 2.0 con PAR obligatorio, PKCE y certificate binding.

## Endpoints

| # | Operación | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 1 | Retrieve | GET | `/.well-known/openid-configuration` | Discovery document |
| 2 | Retrieve | GET | `/.well-known/jwks.json` | Claves públicas (JWKS) |
| 3 | Execute | POST | `/par` | Pushed Authorization Request |
| 4 | Execute | GET | `/authorize` | Inicio flujo de autorización |
| 5 | Execute | POST | `/token` | Emitir/renovar tokens |
| 6 | Execute | POST | `/revoke` | Revocar token |
| 7 | Execute | POST | `/introspect` | Introspección de token |
| 8 | Retrieve | GET | `/userinfo` | Info del usuario autenticado |

## Grant Types Soportados

| Grant Type | Uso |
|---|---|
| `authorization_code` | Acceso a datos del usuario (con consent) |
| `client_credentials` | Operaciones TPP-to-server (crear consents) |
| `refresh_token` | Renovar access token |

## Seguridad (FAPI 2.0 Mandatory)

- PAR obligatorio (no params en URL)
- PKCE S256 obligatorio
- Client auth: `private_key_jwt` o `tls_client_auth`
- Token binding: `cnf.x5t#S256`
- Response type: solo `code`
- Signing: PS256 o ES256
- Token lifetime: access 15min, refresh 90 días con rotation

## Dependencias

- MS-authorization-server (lógica core)
- MS-consent-engine (vincular consent con token)
- Directory Service (validar TPP certificates)
- Identity Provider (SCA del usuario)
