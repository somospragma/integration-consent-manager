# Integration Consent Manager

[![Chapter](https://img.shields.io/badge/Chapter-Integration-blue)]()
[![Open Finance](https://img.shields.io/badge/Standard-Open%20Finance-green)]()
[![FAPI 2.0](https://img.shields.io/badge/Security-FAPI%202.0-orange)]()

## Descripción

Plataforma de gestión de consentimientos para ecosistemas de **Open Finance**. Permite capturar, autorizar, gestionar y revocar el consentimiento del usuario para el acceso, uso y compartición de su información financiera por parte de entidades terceras (TPPs).

Alineado con:
- Decreto 0368 de 2026 (Colombia)
- FAPI 2.0 Security Profile
- Open Banking API v4.0
- ISO 20022

## Arquitectura

```
                    ┌──────────────────────┐
                    │   Developer Portal   │
                    │   (GitHub Pages)     │
                    └──────────┬───────────┘
                               │
┌──────────────────────────────▼───────────────────────────────┐
│                      API Gateway (mTLS + JWT)                 │
├──────────────────────────────────────────────────────────────┤
│  API-consent-lifecycle │ API-consent-authorization            │
│  API-consent-query     │ API-consent-admin                    │
├──────────────────────────────────────────────────────────────┤
│  MS-consent-engine     │ MS-audit-trail                       │
│  MS-notification-dispatcher │ MS-permission-registry          │
├──────────────────────────────────────────────────────────────┤
│  ORCH-consent-flow     │ ORCH-payment-consent                 │
├──────────────────────────────────────────────────────────────┤
│  PostgreSQL (Aurora)   │ Redis (ElastiCache) │ Kafka (MSK)    │
└──────────────────────────────────────────────────────────────┘
```

## Proveedores Integrados

| Herramienta | Rol |
|---|---|
| Raidiam | Directory + PKI + DCR |
| Authlete / Curity / Cloudentity / Ping | Authorization Server FAPI 2.0 |
| Transmit Security | SCA: Biometrics + Passkeys |
| ConnectID | Identity Verification (KYC) |

## Developer Portal

🌐 **[Ver Portal](https://somospragma.github.io/integration-consent-manager/)**

Portal interactivo con Swagger UI para probar las APIs directamente.

```bash
# 1. Levantar dependencias
docker compose up -d

# 2. Compilar
cd services/MS-consent-engine && mvn clean package -DskipTests

# 3. Ejecutar
java -jar target/ms-consent-engine-1.0.0-SNAPSHOT.jar

# 4. Probar
curl http://localhost:8080/actuator/health
```

## Tecnologías

| Componente | Tecnología |
|---|---|
| Lenguaje | Java 21 + Spring Boot 3.3 |
| Base de datos | PostgreSQL 16 (Aurora Serverless v2) |
| Cache | Redis 7 (ElastiCache Serverless) |
| Mensajería | Kafka (MSK Serverless) |
| Contenedores | Docker + Kubernetes (EKS) |
| IaC | Terraform |
| Seguridad | FAPI 2.0, mTLS, OAuth 2.0, JWT |

## Documentación

| Documento | Descripción |
|---|---|
| [Catálogo de APIs](docs/services/api-catalog.md) | 11 APIs, 49+ endpoints, BIAN/ISO 20022/FAPI 2.0 |
| [Arquitectura](docs/architecture/architecture.md) | Diagramas de componentes e infraestructura |
| [Authorization Server](docs/architecture/authorization-server-services.md) | FAPI 2.0, PAR, tokens, DCR |
| [API Management](docs/architecture/api-management-services.md) | Gateway, mTLS, rate limiting |
| [Seguridad](docs/security/api-security.md) | Modelo de seguridad completo |
| [Proveedores](docs/architecture/identity-providers-integration.md) | Authlete, Curity, Raidiam, etc. |
| [Servicios](docs/services/services-definition.md) | Definición técnica de microservicios |
| [ADRs](docs/adrs/) | Decisiones de arquitectura |
| [Costos](docs/architecture/cost-analysis.md) | Análisis de costos (~$650/mes prod) |

## Equipo

Integration Chapter — Pragma
