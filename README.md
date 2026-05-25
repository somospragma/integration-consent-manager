# Integration Consent Manager

[![Chapter](https://img.shields.io/badge/Chapter-Integration-blue)]()
[![Open Finance](https://img.shields.io/badge/Standard-Open%20Finance-green)]()
[![FAPI 2.0](https://img.shields.io/badge/Security-FAPI%202.0-orange)]()

## Descripción

Plataforma de gestión de consentimientos para ecosistemas de **Open Finance**. Permite capturar, autorizar, gestionar y revocar el consentimiento del usuario para el acceso, uso y compartición de su información financiera por parte de entidades terceras (TPPs).

Alineado con:
- Decreto 0368 de 2026 (Colombia)
- FAPI 2.0 Security Profile
- Open Banking UK Read/Write API v4.0
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

## Estructura del Repositorio

```
├── docs/                    # Documentación completa
│   ├── architecture/        # Diagramas y decisiones
│   ├── adrs/                # Architecture Decision Records
│   ├── security/            # Modelo de seguridad
│   ├── services/            # Definición de servicios
│   └── proposals/           # Propuestas de negocio
├── apis/                    # OpenAPI Specs por API
│   ├── API-consent-lifecycle/
│   ├── API-consent-authorization/
│   ├── API-consent-query/
│   └── API-consent-admin/
├── services/                # Código fuente de microservicios
│   ├── MS-consent-engine/
│   ├── MS-audit-trail/
│   ├── MS-notification-dispatcher/
│   └── MS-permission-registry/
├── orchestrators/           # Orquestadores (Sagas)
│   ├── ORCH-consent-flow/
│   └── ORCH-payment-consent/
├── infrastructure/          # Terraform (AWS, multi-nube ready)
├── developer-portal/        # Portal (Redoc + GitHub Pages)
└── docker-compose.yml       # Desarrollo local
```

## Nomenclatura

| Prefijo | Tipo | Descripción |
|---|---|---|
| `API-` | API REST | Interfaz expuesta (OpenAPI spec) |
| `MS-` | Microservicio | Servicio con responsabilidad única |
| `ORCH-` | Orquestador | Coordina múltiples MS (Saga pattern) |

## Quick Start

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
| [Catálogo de APIs](docs/services/api-catalog.md) | 49 endpoints, seguridad BIAN/ISO 20022/FAPI 2.0 |
| [Arquitectura](docs/architecture/architecture.md) | Diagramas y componentes |
| [Seguridad](docs/security/api-security.md) | Modelo de seguridad completo |
| [Servicios](docs/services/services-definition.md) | Definición técnica de servicios |
| [ADRs](docs/adrs/) | Decisiones de arquitectura |
| [Costos](docs/architecture/cost-analysis.md) | Análisis de costos |
| [Propuesta Lite](docs/proposals/proposal-lite.md) | Modelo de aceleradores |

## Equipo

Integration Chapter — Pragma
