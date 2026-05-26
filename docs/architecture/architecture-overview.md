# Arquitectura de Plataforma Open Finance Multi-Nube

## 1. Visión General

Plataforma tecnológica que habilita el ecosistema de Open Finance, permitiendo a entidades financieras consumir aceleradores (APIs) de pagos, agregación de cuentas y gestión de consentimientos, desplegable en AWS, Azure y GCP con una abstracción unificada.

## 2. Principios de Arquitectura

| Principio | Descripción |
|---|---|
| Cloud Agnostic | Mismo código de infraestructura despliega en cualquier nube |
| API-First | Contratos definidos antes de implementación |
| Security by Design | mTLS, FAPI 2.0, cifrado en reposo y tránsito |
| Observable | Trazabilidad completa de cada operación |
| Escalable | Diseño multi-región, preparado para expansión internacional |
| Compliance-Driven | Alineado con Decreto 0368/2026 y SFC |

## 3. Capas de la Arquitectura

### 3.1 Capa de Presentación — Developer Portal

Portal estático donde entidades financieras:
- Descubren los aceleradores disponibles (APIs)
- Leen documentación técnica (OpenAPI specs)
- Prueban en sandbox con datos mock
- Gestionan sus credenciales y API keys
- Entienden el flujo de consentimiento

**Tecnología:** Sitio estático (React/Astro) + CDN + Sandbox aislado

### 3.2 Capa de Gateway — API Management

Punto de entrada único para todas las peticiones:
- Terminación TLS/mTLS
- Autenticación y autorización (validación de tokens FAPI 2.0)
- Rate limiting y throttling
- Routing inteligente a microservicios
- Versionamiento de APIs
- Logging de auditoría

**Tecnología:** Envoy Proxy / NGINX como Ingress Controller + Istio Service Mesh

### 3.3 Capa de Servicios — Microservicios Core

| Servicio | Responsabilidad |
|---|---|
| Consent Manager | Ciclo de vida del consentimiento (crear, consultar, revocar) |
| Authorization Server | Emisión de tokens OAuth2/FAPI 2.0, validación SCA |
| Payment Initiation API | Orquestación de pagos bajo consentimiento |
| Account Aggregation API | Consolidación de información financiera multi-entidad |
| Directory Service | Registro de entidades participantes y ruteo |

### 3.4 Capa de Datos

| Componente | Uso | Tecnología |
|---|---|---|
| Base relacional | Consentimientos, auditoría, entidades | PostgreSQL |
| Cache | Tokens, sesiones, rate limiting | Redis |
| Event Bus | Comunicación asíncrona entre servicios | Kafka/NATS |
| Object Storage | Documentos, logs de auditoría | S3/Blob/GCS |

### 3.5 Capa de Infraestructura

| Componente | AWS | Azure | GCP |
|---|---|---|---|
| Kubernetes | EKS | AKS | GKE |
| Networking | VPC + ALB | VNet + Azure LB | VPC + Cloud LB |
| DNS | Route53 | Azure DNS | Cloud DNS |
| KMS | AWS KMS | Azure Key Vault | Cloud KMS |
| Container Registry | ECR | ACR | Artifact Registry |
| DB Managed | RDS PostgreSQL | Azure DB for PostgreSQL | Cloud SQL |
| Cache | ElastiCache | Azure Cache for Redis | Memorystore |
| CDN | CloudFront | Azure CDN | Cloud CDN |

## 4. Diagrama de Componentes

```mermaid
graph TB
    subgraph "Developer Portal"
        DP[Portal Estático\nReact/Astro]
        SB[Sandbox\nAmbiente Aislado]
        DOCS[API Docs\nOpenAPI Specs]
    end

    subgraph "API Gateway Layer"
        GW[Envoy/NGINX Ingress]
        WAF[Web Application Firewall]
        MTLS[mTLS Termination]
    end

    subgraph "Service Mesh - Istio"
        subgraph "Core Services"
            CM[Consent Manager]
            AS[Authorization Server\nFAPI 2.0]
            PI[Payment Initiation API]
            AA[Account Aggregation API]
            DS[Directory Service]
        end
    end

    subgraph "Data Layer"
        PG[(PostgreSQL)]
        RD[(Redis)]
        KF[Kafka/NATS]
        OBJ[(Object Storage)]
    end

    subgraph "Infrastructure - Multi-Cloud"
        subgraph "AWS"
            EKS[EKS Cluster]
        end
        subgraph "Azure"
            AKS[AKS Cluster]
        end
        subgraph "GCP"
            GKE[GKE Cluster]
        end
    end

    DP --> GW
    SB --> GW
    GW --> WAF
    WAF --> MTLS
    MTLS --> CM
    MTLS --> AS
    MTLS --> PI
    MTLS --> AA
    MTLS --> DS

    CM --> PG
    CM --> KF
    AS --> RD
    AS --> PG
    PI --> KF
    PI --> PG
    AA --> PG
    DS --> PG

    CM --> AS
    PI --> CM
    PI --> AS
    AA --> CM
    AA --> AS
```

## 5. Flujo Principal — Consumo de API por Tercero

```mermaid
sequenceDiagram
    participant E as Entidad (Tercero)
    participant P as Developer Portal
    participant GW as API Gateway
    participant AS as Auth Server
    participant CM as Consent Manager
    participant API as API de Pagos/Agregación

    E->>P: 1. Registro y obtención de credenciales
    P->>E: Client ID + Client Secret

    E->>AS: 2. Solicitud de token (client_credentials + private_key_jwt)
    AS->>AS: Validar certificado mTLS + credenciales
    AS->>E: Access Token (JWT firmado)

    E->>GW: 3. Solicitar consentimiento del usuario
    GW->>GW: Validar token + mTLS
    GW->>CM: Forward request
    CM->>CM: Crear consentimiento pendiente
    CM->>E: Consent ID + Redirect URL

    E->>CM: 4. Usuario autoriza consentimiento
    CM->>CM: Registrar autorización
    CM->>AS: Notificar consentimiento activo
    AS->>E: Authorization Code

    E->>AS: 5. Intercambiar code por access token con scope
    AS->>E: Access Token con permisos del consentimiento

    E->>GW: 6. Consumir API (con token + consentimiento)
    GW->>GW: Validar token + scope + consentimiento vigente
    GW->>API: Forward request autorizada
    API->>E: Respuesta con datos
```

## 6. Seguridad

### Estándares implementados
- **FAPI 2.0** — Financial-grade API Security Profile
- **OAuth 2.0** con PKCE y private_key_jwt
- **mTLS** — Mutual TLS entre todas las entidades
- **JWT firmado** — Tokens con firma RSA/EC
- **SCA** — Strong Customer Authentication para consentimientos

### Controles
- Cifrado en reposo (KMS por nube)
- Cifrado en tránsito (TLS 1.3)
- Secrets en HashiCorp Vault
- RBAC en Kubernetes
- Network Policies (zero-trust entre pods)
- Audit logs inmutables con timestamp y enmascaramiento

## 7. Observabilidad

| Capa | Herramienta | Propósito |
|---|---|---|
| Métricas | Prometheus + Grafana | SLAs, latencia, throughput |
| Logs | Loki / OpenSearch | Logs centralizados y auditoría |
| Tracing | Tempo / Jaeger | Tracing distribuido entre servicios |
| Alertas | Alertmanager | Notificación de incidentes |
| Instrumentación | OpenTelemetry | Estándar portable de telemetría |

## 8. Multi-Región y Escalabilidad

- **Región primaria:** Colombia (São Paulo AWS/GCP, Brazil South Azure como más cercanas)
- **Diseño:** Preparado para multi-región con replicación de datos
- **Escalamiento:** HPA (Horizontal Pod Autoscaler) por servicio
- **Failover:** DNS-based failover entre regiones/nubes
