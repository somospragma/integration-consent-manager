# MS-authorization-server

**BIAN Domain:** Party Authentication

## Descripción

Microservicio del Authorization Server FAPI 2.0. Emite tokens JWT sender-constrained, gestiona flujos OAuth 2.0, autenticación de clientes (private_key_jwt), y registro dinámico (DCR).

## Responsabilidad

- Emitir access tokens JWT con certificate binding (cnf)
- Gestionar flujo OAuth 2.0 (PAR → authorize → token)
- Autenticar TPPs via private_key_jwt
- Gestionar refresh tokens con rotation
- Revocar tokens
- Introspección de tokens (para API Gateway)
- Dynamic Client Registration (DCR)
- Gestión de claves (JWKS, rotación)
- Integración con Consent Manager (vincular consent_id en token)
- Integración con Identity Provider (SCA del usuario)

## Endpoints que expone

| Endpoint | Descripción |
|---|---|
| `/.well-known/openid-configuration` | Discovery |
| `/.well-known/jwks.json` | Claves públicas |
| `/par` | Pushed Authorization Request |
| `/authorize` | Flujo de autorización |
| `/token` | Emisión de tokens |
| `/revoke` | Revocación |
| `/introspect` | Introspección |
| `/userinfo` | Info del usuario |
| `/register` | DCR (CRUD) |

## Stack

- Java 21 + Spring Boot 3.x
- Spring Security OAuth2 Authorization Server
- Nimbus JOSE+JWT (crypto)
- PostgreSQL (clients, grants, codes)
- Redis (sessions, JTI tracking, PAR request_uri)
- HashiCorp Vault / AWS KMS (signing keys)

## Configuración

| Variable | Descripción | Default |
|---|---|---|
| `AUTH_ISSUER` | Issuer URL | https://auth.openfinance.example.com |
| `AUTH_ACCESS_TOKEN_TTL` | Access token lifetime | 900 (15 min) |
| `AUTH_REFRESH_TOKEN_TTL` | Refresh token lifetime | 7776000 (90 días) |
| `AUTH_CODE_TTL` | Authorization code lifetime | 60 (1 min) |
| `AUTH_PAR_TTL` | PAR request_uri lifetime | 60 (1 min) |
| `AUTH_SIGNING_ALG` | Signing algorithm | PS256 |
| `AUTH_KEY_ROTATION_DAYS` | Key rotation interval | 90 |
| `CONSENT_MANAGER_URL` | URL del Consent Manager | http://ms-consent-engine:8080 |
| `IDP_URL` | Identity Provider URL | — |
| `DB_URL` | PostgreSQL | — |
| `REDIS_URL` | Redis | — |

## Puertos

| Puerto | Uso |
|---|---|
| 8080 | API principal |
| 8081 | Actuator (health, metrics) |

## Seguridad FAPI 2.0 Checklist

- [x] PAR obligatorio
- [x] PKCE S256 obligatorio
- [x] Sender-constrained tokens (cnf)
- [x] private_key_jwt (no client_secret)
- [x] Response type: solo code
- [x] Issuer identification en response
- [x] Short-lived auth codes (60s, single-use)
- [x] Refresh token rotation
- [x] PS256/ES256 signing
- [x] Exact redirect_uri matching
