# Arquitectura del Consent Manager Pragma

## 1. Diagrama de Arquitectura General

```mermaid
graph TB
    subgraph "Externos"
        TPP1[TPP - Fintech]
        TPP2[TPP - Banco]
        USER[Usuario Final]
        DEV[Desarrollador<br/>Developer Portal]
    end

    subgraph "Edge Layer"
        GH_PAGES[GitHub Pages<br/>Developer Portal]
        WAF[AWS WAF]
        ALB[Application Load Balancer<br/>+ mTLS termination]
    end

    subgraph "AWS EKS Cluster"
        subgraph "Ingress"
            ISTIO_GW[Istio Gateway<br/>Token + Consent validation]
        end

        subgraph "APIs (Namespace: consent-manager)"
            API_LC[API-consent-lifecycle<br/>:8080]
            API_AUTH[API-consent-authorization<br/>:8080]
            API_QRY[API-consent-query<br/>:8080]
            API_ADM[API-consent-admin<br/>:8080]
        end

        subgraph "Microservicios"
            MS_ENGINE[MS-consent-engine<br/>Core Logic + State Machine]
            MS_AUDIT[MS-audit-trail<br/>Immutable Logs]
            MS_NOTIF[MS-notification-dispatcher<br/>Webhooks]
            MS_PERM[MS-permission-registry<br/>Permission Catalog]
        end

        subgraph "Orquestadores"
            ORCH_FLOW[ORCH-consent-flow<br/>Account Consent Saga]
            ORCH_PAY[ORCH-payment-consent<br/>Payment Consent Saga]
        end
    end

    subgraph "Data Layer (AWS Managed - Serverless)"
        AURORA[(Aurora Serverless v2<br/>PostgreSQL 16)]
        REDIS[(ElastiCache Serverless<br/>Redis 7)]
        MSK[MSK Serverless<br/>Kafka]
        SECRETS[Secrets Manager]
    end

    subgraph "Observability"
        CW[CloudWatch Logs]
        PROM[Prometheus<br/>in-cluster]
        GRAFANA[Grafana<br/>Dashboards]
        SNS[SNS Alerts]
    end

    %% External connections
    DEV --> GH_PAGES
    TPP1 -->|mTLS| WAF
    TPP2 -->|mTLS| WAF
    USER --> ORCH_FLOW

    %% Edge to cluster
    WAF --> ALB
    ALB --> ISTIO_GW

    %% Gateway to APIs
    ISTIO_GW --> API_LC
    ISTIO_GW --> API_AUTH
    ISTIO_GW --> API_QRY
    ISTIO_GW --> API_ADM

    %% APIs to Microservices
    API_LC --> MS_ENGINE
    API_AUTH --> MS_ENGINE
    API_QRY --> MS_ENGINE
    API_ADM --> MS_ENGINE
    API_LC --> MS_PERM

    %% Orchestrators
    ORCH_FLOW --> MS_ENGINE
    ORCH_PAY --> MS_ENGINE

    %% Microservices to Data
    MS_ENGINE --> AURORA
    MS_ENGINE --> REDIS
    MS_ENGINE --> MSK
    MS_AUDIT --> AURORA
    MS_AUDIT --> MSK
    MS_NOTIF --> MSK
    MS_NOTIF --> AURORA
    MS_PERM --> AURORA
    MS_PERM --> REDIS
    ORCH_FLOW --> REDIS
    ORCH_PAY --> REDIS

    %% Secrets
    MS_ENGINE --> SECRETS
    MS_AUDIT --> SECRETS

    %% Observability
    MS_ENGINE --> PROM
    MS_AUDIT --> CW
    PROM --> GRAFANA
    CW --> SNS
```

---

## 2. Diagrama de Componentes Detallado

```mermaid
graph LR
    subgraph "API Layer"
        direction TB
        A1[API-consent-lifecycle<br/>POST/GET/DELETE /consents]
        A2[API-consent-authorization<br/>validate/authorize/reject/active]
        A3[API-consent-query<br/>list/history/by-user/by-tpp]
        A4[API-consent-admin<br/>search/bulk-revoke/metrics/config]
    end

    subgraph "Service Layer"
        direction TB
        S1[MS-consent-engine<br/>• State Machine<br/>• CRUD<br/>• Expiration Job<br/>• Event Publisher]
        S2[MS-audit-trail<br/>• Hash Chain<br/>• Kafka Consumer<br/>• Integrity Check]
        S3[MS-notification-dispatcher<br/>• Webhook Registry<br/>• HMAC Signing<br/>• Retry + DLQ]
        S4[MS-permission-registry<br/>• Permission Catalog<br/>• Endpoint Mapping<br/>• Validation]
    end

    subgraph "Orchestration Layer"
        direction TB
        O1[ORCH-consent-flow<br/>• PAR → Auth → Token<br/>• Saga + Compensation<br/>• Circuit Breaker]
        O2[ORCH-payment-consent<br/>• Consent → Funds → Pay<br/>• Single-use consume<br/>• Payment execution]
    end

    A1 --> S1
    A2 --> S1
    A3 --> S1
    A4 --> S1
    A1 --> S4
    O1 --> S1
    O2 --> S1
    S1 -->|Events| S2
    S1 -->|Events| S3
```

---

## 3. Diagrama de Infraestructura AWS

```mermaid
graph TB
    subgraph "AWS Region: sa-east-1"
        subgraph "VPC: 10.x.0.0/16"
            subgraph "Public Subnets (2 AZs)"
                ALB[ALB<br/>+ WAF]
                NAT[NAT Gateway]
            end

            subgraph "Private Subnets (2 AZs)"
                subgraph "EKS Cluster"
                    NG[Node Group<br/>t3.medium (Spot/OnDemand)<br/>2-10 nodes]
                end
            end

            subgraph "Database Subnets (isolated)"
                AURORA[(Aurora Serverless v2<br/>0.5-16 ACU<br/>PostgreSQL 16)]
            end
        end

        subgraph "Serverless (VPC-connected)"
            ECACHE[(ElastiCache Serverless<br/>Redis 7<br/>2-10 GB)]
            KAFKA[MSK Serverless<br/>Kafka<br/>Pay per throughput]
        end

        subgraph "Management"
            SM[Secrets Manager]
            KMS[KMS<br/>Encryption Keys]
            ECR[ECR<br/>Container Images]
            CW_LOGS[CloudWatch Logs<br/>14d dev / 5y audit]
        end

        subgraph "Edge"
            GH[GitHub Pages<br/>Developer Portal<br/>$0/mes]
        end
    end

    ALB --> NG
    NG --> AURORA
    NG --> ECACHE
    NG --> KAFKA
    NG --> SM
    NAT --> NG
```

---

## 4. Diagrama de Flujo de Datos

```mermaid
flowchart LR
    subgraph "Entrada"
        REQ[Request TPP<br/>mTLS + JWT]
    end

    subgraph "Seguridad"
        WAF_F[WAF Filter]
        MTLS_F[mTLS Validation]
        JWT_F[JWT Validation]
        CONSENT_F[Consent Check]
        RATE_F[Rate Limit]
    end

    subgraph "Procesamiento"
        CTRL[Controller]
        SVC[Service]
        REPO[Repository]
    end

    subgraph "Persistencia"
        DB[(PostgreSQL)]
        CACHE[(Redis)]
        QUEUE[Kafka]
    end

    subgraph "Salida"
        RES[Response]
        EVENT[Event Published]
        WEBHOOK[Webhook Dispatched]
        LOG[Audit Logged]
    end

    REQ --> WAF_F --> MTLS_F --> JWT_F --> CONSENT_F --> RATE_F --> CTRL
    CTRL --> SVC --> REPO
    REPO --> DB
    REPO --> CACHE
    SVC --> QUEUE
    CTRL --> RES
    QUEUE --> EVENT
    EVENT --> WEBHOOK
    EVENT --> LOG
```

---

## 5. Comunicación entre Servicios

```mermaid
graph TB
    subgraph "Comunicación Síncrona (HTTP/REST)"
        API_LC -->|HTTP| MS_ENGINE
        API_AUTH -->|HTTP| MS_ENGINE
        ORCH_FLOW -->|HTTP| MS_ENGINE
        ORCH_FLOW -->|HTTP| AUTH_SERVER[Auth Server<br/>externo]
        ORCH_PAY -->|HTTP| PAYMENT_SVC[Payment Service<br/>externo]
        ISTIO_GW -->|HTTP| MS_PERM
    end

    subgraph "Comunicación Asíncrona (Kafka Events)"
        MS_ENGINE -->|consent.created| TOPIC[consent-events]
        MS_ENGINE -->|consent.authorized| TOPIC
        MS_ENGINE -->|consent.revoked| TOPIC
        TOPIC -->|consume| MS_AUDIT
        TOPIC -->|consume| MS_NOTIF
    end

    subgraph "Cache (Redis)"
        MS_ENGINE -->|read/write| REDIS_CONSENT[consent:{id}]
        ORCH_FLOW -->|read/write| REDIS_FLOW[consent-flow:{id}]
        ORCH_PAY -->|read/write| REDIS_PAY[payment-flow:{id}]
        MS_PERM -->|read| REDIS_PERM[permissions cache]
    end
```

---

## 6. Sizing y Recursos por Ambiente

### Dev (~$180/mes)

| Recurso | Configuración | Costo estimado |
|---|---|---|
| EKS Control Plane | 1 cluster | $73 |
| EKS Nodes | 2x t3.medium Spot | $30 |
| Aurora Serverless | 0.5 ACU min | $45 |
| ElastiCache Serverless | 2GB max | $10 |
| MSK Serverless | Low throughput | $15 |
| NAT Gateway | 1 (single AZ) | $35 |
| Secrets Manager | 5 secrets | $3 |
| **Total** | | **~$211** |

### Production (~$650/mes)

| Recurso | Configuración | Costo estimado |
|---|---|---|
| EKS Control Plane | 1 cluster | $73 |
| EKS Nodes | 3x m6i.large On-Demand | $280 |
| Aurora Serverless | 1-16 ACU + reader | $120 |
| ElastiCache Serverless | 10GB max | $40 |
| MSK Serverless | Medium throughput | $50 |
| NAT Gateway | 3 (multi-AZ) | $105 |
| WAF | Standard rules | $20 |
| Secrets Manager | 10 secrets | $5 |
| CloudWatch | Logs + alarms | $30 |
| **Total** | | **~$723** |

---

## 7. Decisiones de Arquitectura

| Decisión | Elección | Razón |
|---|---|---|
| Compute | EKS (Kubernetes) | Portable, estándar de industria |
| Database | Aurora Serverless v2 | Escala automático, paga por uso |
| Cache | ElastiCache Serverless | Sin nodos fijos, paga por uso |
| Messaging | MSK Serverless | Kafka sin administrar brokers |
| Service Mesh | Istio | mTLS automático pod-to-pod |
| Observability | Prometheus + CloudWatch | Open source + nativo AWS |
| Secrets | Secrets Manager + KMS | Rotación automática, IAM integration |
| Developer Portal | GitHub Pages + Redoc | $0/mes, open source |
| IaC | Terraform | Agnóstico, portable a otra nube |
| CI/CD | GitHub Actions | Integrado con el repo |

---

## 8. Escalabilidad

```mermaid
graph LR
    subgraph "Horizontal Scaling"
        HPA[HPA<br/>CPU > 70% → scale up]
        CA[Cluster Autoscaler<br/>Pods pending → add node]
        AURORA_S[Aurora<br/>0.5 → 16 ACU auto]
        REDIS_S[Redis<br/>Auto-scale ECPU]
    end

    subgraph "Capacidad Estimada"
        C1[Dev: 100 req/seg]
        C2[Prod: 5,000 req/seg]
        C3[Peak: 10,000 req/seg<br/>con autoscaling]
    end

    HPA --> C1
    HPA --> C2
    CA --> C3
    AURORA_S --> C3
    REDIS_S --> C3
```

| Métrica | Dev | Prod | Peak |
|---|---|---|---|
| Requests/seg | 100 | 5,000 | 10,000 |
| Consentimientos activos | 1K | 500K | 2M |
| Pods MS-consent-engine | 2 | 5-10 | 20 |
| Aurora ACUs | 0.5 | 4-8 | 16 |
| Redis ECPU/seg | 100 | 5,000 | 10,000 |
