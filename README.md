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

```mermaid
graph TB
    subgraph "Actores"
        TPP[TPP - Fintech/Banco]
        USER[Usuario Final]
        ADMIN[Administrador]
    end

    subgraph "Consent Manager"
        subgraph "APIs Externas"
            A1[API Consent Lifecycle]
            A2[API Consent Query]
            A3[API Webhooks]
            A4[API Permissions]
        end
        subgraph "APIs Internas"
            A5[API Authorization]
            A6[API Admin]
            A7[API Audit]
        end
        subgraph "Microservicios"
            MS1[MS Consent Engine]
            MS2[MS Audit Trail]
            MS3[MS Notifications]
            MS4[MS Permission Registry]
            MS5[MS Authorization Server]
        end
    end

    subgraph "Infraestructura"
        DB[(PostgreSQL)]
        CACHE[(Redis)]
        KAFKA[Kafka]
    end

    TPP -->|Crear/Consultar/Revocar| A1
    TPP -->|Buscar consents| A2
    TPP -->|Registrar callbacks| A3
    TPP -->|Ver permisos| A4
    USER -->|Autorizar/Rechazar| A5
    ADMIN -->|Gestionar/Monitorear| A6
    ADMIN -->|Auditar| A7

    A1 --> MS1
    A2 --> MS1
    A3 --> MS3
    A4 --> MS4
    A5 --> MS1
    A5 --> MS5
    A6 --> MS1
    A7 --> MS2

    MS1 --> DB
    MS1 --> CACHE
    MS1 --> KAFKA
    MS2 --> DB
    MS3 --> KAFKA
    MS5 --> CACHE
```

### Roles y Permisos

| Actor | Qué puede hacer | APIs que usa |
|---|---|---|
| **TPP** | Crear, consultar y revocar consentimientos | Lifecycle, Query, Webhooks, Permissions |
| **Usuario** | Autorizar o rechazar consentimientos, revocar | Authorization (via Auth Server) |
| **Administrador** | Búsqueda avanzada, revocación masiva, métricas, auditoría | Admin, Audit |

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

## Equipo

Integration Chapter — Pragma
