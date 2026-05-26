# Diagramas de Arquitectura — Open Finance Multi-Nube

## 1. Arquitectura de Infraestructura Multi-Nube

```mermaid
graph TB
    subgraph "Capa de Abstracción - Terraform"
        TF[Terraform Modules - Cloud Agnostic]
    end

    subgraph "AWS - Region sa-east-1"
        subgraph "VPC AWS"
            ALB_AWS[Application Load Balancer]
            subgraph "EKS Cluster"
                EKS_NG[Node Group - Auto Scaling]
                EKS_PODS[Pods - Microservicios]
            end
            RDS_AWS[(RDS PostgreSQL - Multi-AZ)]
            EC_AWS[(ElastiCache Redis)]
            S3_AWS[(S3 - Logs/Docs)]
        end
        ECR_AWS[ECR - Container Registry]
        KMS_AWS[AWS KMS]
        CF_AWS[CloudFront - Portal CDN]
    end

    subgraph "Azure - Region Brazil South"
        subgraph "VNet Azure"
            ALB_AZ[Azure Load Balancer]
            subgraph "AKS Cluster"
                AKS_NG[Node Pool - Auto Scaling]
                AKS_PODS[Pods - Microservicios]
            end
            PG_AZ[(Azure DB PostgreSQL - HA)]
            RD_AZ[(Azure Cache Redis)]
            BLOB_AZ[(Blob Storage)]
        end
        ACR_AZ[ACR - Container Registry]
        KV_AZ[Azure Key Vault]
        CDN_AZ[Azure CDN - Portal]
    end

    subgraph "GCP - Region southamerica-east1"
        subgraph "VPC GCP"
            GLB_GCP[Cloud Load Balancer]
            subgraph "GKE Cluster"
                GKE_NG[Node Pool - Auto Scaling]
                GKE_PODS[Pods - Microservicios]
            end
            SQL_GCP[(Cloud SQL PostgreSQL - HA)]
            MEM_GCP[(Memorystore Redis)]
            GCS_GCP[(GCS - Logs/Docs)]
        end
        AR_GCP[Artifact Registry]
        KMS_GCP[Cloud KMS]
        CDN_GCP[Cloud CDN - Portal]
    end

    TF --> ALB_AWS
    TF --> ALB_AZ
    TF --> GLB_GCP
```

## 2. Flujo de Consentimiento Completo

```mermaid
sequenceDiagram
    participant U as Usuario Final
    participant TPP as Entidad Tercera (TPP)
    participant GW as API Gateway
    participant CM as Consent Manager
    participant AS as Authorization Server
    participant BANK as Entidad Financiera (ASPSP)

    Note over TPP,BANK: Fase 1 - Solicitud de Consentimiento
    TPP->>GW: POST /consents (scope, permissions, expiry)
    GW->>GW: Validar mTLS + Token TPP
    GW->>CM: Crear consentimiento
    CM->>CM: Generar Consent ID + Estado: AWAITING_AUTHORIZATION
    CM-->>TPP: 201 Created {consentId, status, redirectUrl}

    Note over U,BANK: Fase 2 - Autorización del Usuario
    TPP->>U: Redirigir a pantalla de autorización
    U->>AS: Acceder a authorize endpoint
    AS->>CM: Obtener detalle del consentimiento
    CM-->>AS: {permissions, TPP info, expiry}
    AS->>U: Mostrar pantalla de autorización (qué datos, con quién, por cuánto)
    U->>AS: Autorizar (SCA - biometría/OTP)
    AS->>CM: Actualizar estado: AUTHORIZED
    AS->>TPP: Authorization Code (redirect)

    Note over TPP,BANK: Fase 3 - Obtención de Token
    TPP->>AS: POST /token (code + private_key_jwt + client_assertion)
    AS->>AS: Validar code + client + consentimiento activo
    AS-->>TPP: Access Token + Refresh Token (scope limitado al consentimiento)

    Note over TPP,BANK: Fase 4 - Consumo de API
    TPP->>GW: GET /accounts (Bearer token)
    GW->>AS: Introspect token
    AS->>CM: Verificar consentimiento vigente
    CM-->>AS: OK - Consentimiento activo
    AS-->>GW: Token válido + scopes
    GW->>BANK: Forward request
    BANK-->>GW: Datos de cuentas
    GW-->>TPP: Respuesta filtrada según permisos

    Note over U,CM: Fase 5 - Revocación (cuando el usuario quiera)
    U->>CM: DELETE /consents/{consentId}
    CM->>CM: Estado: REVOKED
    CM->>AS: Invalidar tokens asociados
    CM-->>U: Consentimiento revocado
```

## 3. Flujo de Iniciación de Pagos

```mermaid
sequenceDiagram
    participant U as Usuario
    participant TPP as Entidad Iniciadora
    participant GW as API Gateway
    participant CM as Consent Manager
    participant AS as Auth Server
    participant PI as Payment Initiation API
    participant BANK as Banco (ASPSP)

    TPP->>GW: POST /payment-consents (monto, beneficiario, concepto)
    GW->>CM: Crear consentimiento de pago
    CM-->>TPP: {paymentConsentId, redirectUrl}

    TPP->>U: Redirigir para autorizar pago
    U->>AS: Autorizar pago (SCA)
    AS->>CM: Consentimiento de pago AUTHORIZED
    AS-->>TPP: Authorization Code

    TPP->>AS: POST /token (code)
    AS-->>TPP: Access Token (scope: payment)

    TPP->>GW: POST /payments (token + paymentConsentId)
    GW->>PI: Iniciar pago
    PI->>CM: Verificar consentimiento de pago vigente
    CM-->>PI: OK
    PI->>BANK: Ejecutar transferencia
    BANK-->>PI: Pago procesado {transactionId}
    PI->>PI: Registrar auditoría
    PI-->>TPP: 201 {paymentId, status: PROCESSING}

    Note over TPP,BANK: Consulta de estado
    TPP->>GW: GET /payments/{paymentId}
    GW->>PI: Consultar estado
    PI-->>TPP: {status: COMPLETED, transactionId}
```

## 4. Flujo de Agregación de Cuentas

```mermaid
sequenceDiagram
    participant U as Usuario
    participant TPP as Agregador
    participant GW as API Gateway
    participant CM as Consent Manager
    participant AS as Auth Server
    participant AA as Account Aggregation API
    participant B1 as Banco 1
    participant B2 as Banco 2

    Note over U,B2: Consentimiento para agregar cuentas de múltiples bancos
    TPP->>GW: POST /consents (scope: accounts, balances, transactions)
    GW->>CM: Crear consentimiento de agregación
    CM-->>TPP: {consentId, redirectUrl}

    U->>AS: Autorizar acceso a información
    AS->>CM: AUTHORIZED
    AS-->>TPP: Access Token

    Note over TPP,B2: Consulta agregada
    TPP->>GW: GET /accounts (token)
    GW->>AA: Solicitar cuentas agregadas
    
    par Consulta paralela a bancos
        AA->>B1: GET /accounts (credenciales interbancarias)
        AA->>B2: GET /accounts (credenciales interbancarias)
    end
    
    B1-->>AA: Cuentas Banco 1
    B2-->>AA: Cuentas Banco 2
    
    AA->>AA: Consolidar + filtrar según permisos del consentimiento
    AA-->>TPP: Vista unificada de cuentas

    TPP->>GW: GET /accounts/{id}/transactions
    GW->>AA: Solicitar movimientos
    AA->>B1: GET /transactions
    B1-->>AA: Movimientos
    AA-->>TPP: Transacciones filtradas
```

## 5. Arquitectura del Developer Portal

```mermaid
graph TB
    subgraph "Developer Portal - Sitio Estático"
        subgraph "Frontend"
            HOME[Landing Page - ¿Qué es Open Finance?]
            CAT[Catálogo de APIs - Aceleradores disponibles]
            DOCS_PAGE[Documentación - OpenAPI Specs interactivas]
            SANDBOX_UI[Sandbox UI - Probar APIs en vivo]
            ONBOARD[Onboarding - Registro de entidad]
            CONSOLE[Consola - API Keys, métricas, logs]
        end
    end

    subgraph "Backend del Portal"
        AUTH_P[Auth Portal - Login entidades]
        KEY_MGR[Key Manager - Generación de credenciales]
        METRICS[Métricas de uso - Dashboard por entidad]
    end

    subgraph "Sandbox Environment"
        SB_GW[Gateway Sandbox]
        SB_MOCK[Mock Services - Datos ficticios]
        SB_CM[Consent Manager - Modo prueba]
    end

    subgraph "Hosting"
        CDN[CDN Global]
        STATIC[Static Storage - S3/Blob/GCS]
    end

    HOME --> CAT
    CAT --> DOCS_PAGE
    DOCS_PAGE --> SANDBOX_UI
    SANDBOX_UI --> SB_GW
    SB_GW --> SB_MOCK
    SB_GW --> SB_CM
    ONBOARD --> AUTH_P
    AUTH_P --> KEY_MGR
    CONSOLE --> METRICS

    CDN --> STATIC
    STATIC --> HOME
```

## 6. Pipeline CI/CD Multi-Nube

```mermaid
graph LR
    subgraph "Source"
        GIT[Git Repository]
    end

    subgraph "Build"
        LINT[Lint + SAST]
        TEST[Unit Tests]
        BUILD[Docker Build]
        SCAN[Image Scan - Vulnerabilidades]
    end

    subgraph "Registry"
        REG[Container Registry - ECR/ACR/AR]
    end

    subgraph "Deploy - GitOps"
        ARGO[ArgoCD]
        subgraph "Ambientes"
            DEV[Dev]
            SBX[Sandbox]
            STG[Staging]
            PROD[Production]
        end
    end

    subgraph "Target Clusters"
        EKS[AWS EKS]
        AKS[Azure AKS]
        GKE[GCP GKE]
    end

    GIT --> LINT
    LINT --> TEST
    TEST --> BUILD
    BUILD --> SCAN
    SCAN --> REG
    REG --> ARGO
    ARGO --> DEV
    DEV --> SBX
    SBX --> STG
    STG --> PROD
    PROD --> EKS
    PROD --> AKS
    PROD --> GKE
```

## 7. Modelo de Seguridad — Zero Trust

```mermaid
graph TB
    subgraph "External"
        TPP[Entidad Tercera]
    end

    subgraph "Perimeter"
        WAF[WAF - Filtrado L7]
        DDOS[DDoS Protection]
        GW[API Gateway - mTLS + Token Validation]
    end

    subgraph "Service Mesh - Zero Trust"
        ISTIO[Istio - mTLS pod-to-pod]
        subgraph "Services"
            S1[Consent Manager]
            S2[Auth Server]
            S3[Payment API]
        end
        NP[Network Policies - Deny All by Default]
    end

    subgraph "Data Security"
        VAULT[HashiCorp Vault - Secrets]
        KMS[KMS - Encryption Keys]
        AUDIT[(Audit Logs - Inmutables)]
    end

    TPP -->|mTLS + JWT| WAF
    WAF --> DDOS
    DDOS --> GW
    GW -->|mTLS| ISTIO
    ISTIO --> S1
    ISTIO --> S2
    ISTIO --> S3
    S1 --> VAULT
    S2 --> VAULT
    S3 --> VAULT
    S1 --> KMS
    S2 --> KMS
    S1 --> AUDIT
    S2 --> AUDIT
    S3 --> AUDIT
    NP --> S1
    NP --> S2
    NP --> S3
```

## 8. Topología de Ambientes

```mermaid
graph TB
    subgraph "Ambientes"
        subgraph "Development"
            DEV_K8S[K8s Cluster Pequeño - 1 nube]
            DEV_DB[(DB Dev)]
        end

        subgraph "Sandbox - Para Terceros"
            SBX_K8S[K8s Cluster - Datos Mock]
            SBX_DB[(DB con datos ficticios)]
            SBX_PORTAL[Portal Sandbox]
        end

        subgraph "Staging"
            STG_K8S[K8s Cluster - Réplica de Prod]
            STG_DB[(DB Staging)]
        end

        subgraph "Production"
            PROD_K8S[K8s Cluster HA - Multi-AZ]
            PROD_DB[(DB HA - Multi-AZ + Backup)]
            PROD_PORTAL[Portal Producción]
        end
    end

    DEV_K8S -->|Promote| SBX_K8S
    SBX_K8S -->|Promote| STG_K8S
    STG_K8S -->|Promote| PROD_K8S
```
