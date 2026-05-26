# Integración con Proveedores de Identidad y Seguridad

## Visión General

El Consent Manager se integra con proveedores especializados de identidad, autorización y trust framework para cumplir con FAPI 2.0 y Open Finance. Cada herramienta cubre un rol específico en la arquitectura.

## Mapa de Herramientas por Rol

```mermaid
graph TB
    subgraph "Trust Framework & Directory"
        RAIDIAM[Raidiam\nDirectorio Central + PKI + DCR]
    end

    subgraph "Authorization Server (FAPI 2.0)"
        AUTHLETE[Authlete\nAuth Server as-a-Service]
        CURITY[Curity Identity Server\nOn-premise / Cloud]
        CLOUDENTITY[Cloudentity\nAuthorization + Consent]
        PING[Ping Identity\nEnterprise IAM + FAPI]
    end

    subgraph "Identity & Authentication (SCA)"
        TRANSMIT[Transmit Security / Mosaic\nPasswordless + Biometrics]
        CONNECTID[ConnectID\nIdentity Verification]
    end

    subgraph "Consent Manager (Pragma)"
        CM[MS-consent-engine]
        AUDIT[MS-audit-trail]
        NOTIF[MS-notification-dispatcher]
    end

    RAIDIAM -->|TPP Registry + Certificates| AUTHLETE
    RAIDIAM -->|TPP Registry + Certificates| CURITY
    RAIDIAM -->|TPP Registry + Certificates| CLOUDENTITY
    RAIDIAM -->|TPP Registry + Certificates| PING

    AUTHLETE -->|Token + Consent binding| CM
    CURITY -->|Token + Consent binding| CM
    CLOUDENTITY -->|Token + Consent binding| CM
    PING -->|Token + Consent binding| CM

    TRANSMIT -->|SCA (biometrics, passkeys)| AUTHLETE
    TRANSMIT -->|SCA (biometrics, passkeys)| CURITY
    CONNECTID -->|Identity verification| AUTHLETE
    CONNECTID -->|Identity verification| CURITY
```

---

## 1. Raidiam — Trust Framework & Directory

**Rol:** Directorio central de participantes, PKI, onboarding automatizado.

| Capacidad | Uso en Consent Manager |
|---|---|
| Directorio de participantes | Registro de TPPs y entidades financieras |
| PKI (certificados) | Emisión de certificados mTLS para TPPs |
| Dynamic Client Registration | Onboarding automatizado de TPPs |
| API Discovery | TPPs descubren APIs disponibles |
| OpenID Federation | Trust chains entre participantes |
| Access control | Roles y permisos por participante |

**Integración:**

```yaml
# Configuración en MS-authorization-server
raidiam:
  directory-url: https://directory.openfinance.example.com
  trust-anchor: https://directory.openfinance.example.com/.well-known/openid-federation
  # Validar SSA (Software Statement Assertion) en DCR
  ssa-issuer: https://directory.openfinance.example.com
  ssa-jwks-uri: https://directory.openfinance.example.com/.well-known/jwks.json
  # Sincronizar participantes
  participants-api: https://directory.openfinance.example.com/participants
  sync-interval-minutes: 15
```

**Puntos de integración:**
- `POST /register` → Validar SSA firmado por Raidiam
- `mTLS validation` → Verificar certificado emitido por Raidiam PKI
- `TPP status check` → Consultar si TPP sigue activo en directorio

---

## 2. Authlete — Authorization Server as-a-Service

**Rol:** Authorization Server FAPI 2.0 certificado, como servicio cloud.

| Capacidad | Uso en Consent Manager |
|---|---|
| FAPI 2.0 certified | Cumplimiento completo del perfil de seguridad |
| PAR, PKCE, JARM | Flujo de autorización seguro |
| Grant Management | Gestión de grants activos |
| Consent binding | Vincular consent_id en tokens |
| Token introspection | Validar tokens en API Gateway |
| DCR | Registro dinámico de clientes |

**Integración:**

```yaml
# Reemplaza MS-authorization-server con Authlete
authlete:
  api-server: https://api.authlete.com
  service-api-key: ${AUTHLETE_API_KEY}
  service-api-secret: ${AUTHLETE_API_SECRET}
  # Consent integration
  consent-claim: "openbanking_intent_id"
  consent-manager-url: http://ms-consent-engine:8080
```

**Cuándo usar Authlete:**
- Si se quiere un Auth Server certificado FAPI sin construirlo
- Reduce tiempo de implementación de 3 meses a 2 semanas
- Ideal para PoC y MVP rápido

---

## 3. Curity Identity Server — On-premise / Cloud

**Rol:** Authorization Server enterprise con consent management integrado (Consentors).

| Capacidad | Uso en Consent Manager |
|---|---|
| FAPI 2.0 certified | Perfil de seguridad financiero |
| Consentors | Módulo nativo de gestión de consentimiento |
| 30+ métodos de autenticación | SCA flexible (biometrics, passkeys, OTP) |
| Claims-based authorization | Permisos granulares en tokens |
| App2App | Flujo mobile nativo |
| CIBA | Client Initiated Backchannel Authentication |

**Integración:**

```yaml
curity:
  base-url: https://idsvr.openfinance.example.com
  admin-url: https://idsvr.openfinance.example.com/admin
  # Consentor configuration
  consentor:
    type: "openbanking"
    consent-api-url: http://ms-consent-engine:8080/v1/consents
    # Curity llama al consent manager durante el flujo de authorize
  # Token profile
  token-profile:
    signing-alg: PS256
    access-token-ttl: 900
    include-consent-id: true
    certificate-binding: true
```

**Cuándo usar Curity:**
- Si se necesita control total on-premise
- Si hay requisitos de residencia de datos estrictos
- Si se necesitan múltiples métodos de SCA

---

## 4. Cloudentity — Authorization + Consent Platform

**Rol:** Plataforma de autorización con consent management nativo y API security.

| Capacidad | Uso en Consent Manager |
|---|---|
| FAPI 2.0 | Perfil de seguridad |
| Consent Management nativo | Puede reemplazar o complementar MS-consent-engine |
| API Security | Políticas de acceso por API |
| Consent analytics | Dashboard de consentimientos |
| Multi-tenant | Múltiples entidades en una instancia |
| Policy engine | Políticas declarativas de acceso |

**Integración:**

```yaml
cloudentity:
  tenant-url: https://pragma.cloudentity.com
  workspace: openfinance
  # Consent storage
  consent:
    mode: "external"  # Usa nuestro MS-consent-engine como storage
    external-api: http://ms-consent-engine:8080/v1
  # O modo "native" para usar consent de Cloudentity
  # consent:
  #   mode: "native"
  #   consent-screen: "custom"
```

**Cuándo usar Cloudentity:**
- Si se quiere consent management + auth server en una sola plataforma
- Si se necesita multi-tenant (múltiples bancos en una instancia)
- Si se quiere policy engine declarativo

---

## 5. Transmit Security / Mosaic — Identity & SCA

**Rol:** Autenticación del usuario (SCA) con biometría, passkeys y detección de fraude.

| Capacidad | Uso en Consent Manager |
|---|---|
| Passwordless auth | Login sin contraseña (passkeys, FIDO2) |
| Biometric verification | Facial, huella, voz |
| Risk-based authentication | Ajustar SCA según riesgo |
| Fraud detection | Detectar intentos fraudulentos |
| Device trust | Verificar dispositivo del usuario |
| Orchestration | Orquestar múltiples factores |

**Integración:**

```yaml
transmit-security:
  api-url: https://api.transmitsecurity.io
  client-id: ${TRANSMIT_CLIENT_ID}
  client-secret: ${TRANSMIT_CLIENT_SECRET}
  # SCA configuration
  sca:
    default-method: "passkey"
    fallback-method: "otp_sms"
    risk-threshold: 0.7  # Si risk > 0.7, pedir factor adicional
  # Se integra como authenticator en Curity/Authlete
  integration-point: "authorization-server-sca"
```

**Cuándo usar Transmit Security:**
- Si se quiere SCA moderna (passkeys, biometría)
- Si se necesita detección de fraude en tiempo real
- Si se quiere eliminar passwords

---

## 6. ConnectID — Identity Verification

**Rol:** Verificación de identidad del usuario (KYC) durante el onboarding y SCA.

| Capacidad | Uso en Consent Manager |
|---|---|
| Identity verification | Verificar identidad real del usuario |
| Document verification | Validar documentos de identidad |
| Liveness detection | Verificar que es una persona real |
| Identity proofing | Nivel de aseguramiento de identidad |
| Reusable identity | Identidad verificada reutilizable |

**Integración:**

```yaml
connectid:
  api-url: https://api.connectid.example.com
  # Se usa durante:
  # 1. Primer consentimiento (verificar identidad del usuario)
  # 2. SCA de alto riesgo (pagos grandes)
  verification-level:
    accounts: "basic"      # Solo verificar que es el titular
    payments-low: "basic"
    payments-high: "enhanced"  # Documento + liveness para pagos > umbral
  threshold-enhanced: 5000000  # COP
```

**Cuándo usar ConnectID:**
- Si se necesita KYC durante el primer consentimiento
- Si el regulador exige verificación de identidad
- Para pagos de alto valor

---

## 7. Ping Identity — Enterprise IAM

**Rol:** Plataforma IAM enterprise con soporte FAPI y federación.

| Capacidad | Uso en Consent Manager |
|---|---|
| PingFederate | Authorization Server FAPI |
| PingAccess | API Gateway con token validation |
| PingDirectory | Directorio de usuarios |
| PingOne | Cloud IAM |
| MFA/SCA | Múltiples factores de autenticación |
| Federation | SAML + OIDC federation |

**Integración:**

```yaml
ping-identity:
  pingfederate-url: https://sso.openfinance.example.com
  # Como Authorization Server
  authorization-server:
    issuer: https://sso.openfinance.example.com
    fapi-profile: "2.0"
    consent-adapter: "custom"
    consent-adapter-url: http://ms-consent-engine:8080/v1
  # Como API Gateway (PingAccess)
  api-gateway:
    token-validation: "local"  # Validar JWT localmente con JWKS
    consent-check: "inline"    # Verificar consent en cada request
```

**Cuándo usar Ping Identity:**
- Si la entidad ya tiene Ping como IAM corporativo
- Si se necesita federación con sistemas legacy
- Para grandes enterprises con múltiples aplicaciones

---

## Matriz de Decisión

| Criterio | Authlete | Curity | Cloudentity | Ping |
|---|---|---|---|---|
| **Deployment** | SaaS | On-prem/Cloud | SaaS/On-prem | On-prem/Cloud |
| **FAPI 2.0 Certified** | ✅ | ✅ | ✅ | ✅ |
| **Consent nativo** | ❌ (externo) | ✅ (Consentors) | ✅ (nativo) | ❌ (adapter) |
| **Time to market** | 2 semanas | 1-2 meses | 3-4 semanas | 2-3 meses |
| **Costo** | Por transacción | Licencia | Por usuario | Licencia |
| **Control** | Bajo (SaaS) | Alto | Medio | Alto |
| **Ideal para** | PoC/MVP | Enterprise on-prem | Multi-tenant | Legacy enterprise |

| Criterio | Raidiam | Transmit | ConnectID |
|---|---|---|---|
| **Rol** | Directory/PKI | SCA/Biometrics | KYC/Identity |
| **Obligatorio** | Sí (directorio) | Opcional (SCA) | Opcional (KYC) |
| **Reemplaza** | Nada (complementa) | SCA básico | KYC manual |
| **Costo** | Por participante | Por autenticación | Por verificación |

---

## Arquitectura Recomendada

```
┌─────────────────────────────────────────────────────────────┐
│                    Raidiam (Directory + PKI)                  │
│         Registro de TPPs, certificados, discovery            │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│          Authorization Server (elegir UNO)                    │
│    Authlete | Curity | Cloudentity | Ping Identity           │
│    ─────────────────────────────────────────────             │
│    PAR + PKCE + Token issuance + DCR + FAPI 2.0             │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│              Consent Manager (Pragma)                         │
│    MS-consent-engine + MS-audit-trail + MS-notifications     │
│    ─────────────────────────────────────────────             │
│    Lifecycle + Query + Webhooks + Permissions + Admin         │
└──────────────────────────┬──────────────────────────────────┘
                           │
┌──────────────────────────▼──────────────────────────────────┐
│          SCA / Identity (opcionales, complementan)            │
│    Transmit Security (biometrics) + ConnectID (KYC)          │
└─────────────────────────────────────────────────────────────┘
```

**Recomendación para arrancar:**
1. **Raidiam** → Obligatorio como directorio central
2. **Authlete** → Para PoC/MVP rápido (SaaS, certificado FAPI)
3. **Curity** → Para producción enterprise (más control)
4. **Transmit Security** → Cuando se quiera SCA moderna (passkeys)
5. **ConnectID** → Cuando el regulador exija KYC
