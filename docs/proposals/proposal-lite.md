# Propuesta LITE — Aceleradores Open Finance con Portal de Demostración

## Resumen Ejecutivo

Entregar los componentes de Open Finance como **aceleradores empaquetados** (Helm Charts + Terraform + Docker) que se despliegan en la infraestructura del cliente, manteniendo un **portal propio de demostración y sandbox** para que el cliente entienda, pruebe y valide antes de implementar.

---

## 1. ¿Qué es un Acelerador?

Un acelerador es un componente de software listo para desplegar que resuelve una necesidad específica del ecosistema Open Finance:

```mermaid
graph LR
    subgraph "Acelerador"
        HC[Helm Chart<br/>Despliegue K8s]
        TF[Terraform Module<br/>Infraestructura]
        DI[Docker Image<br/>Microservicio]
        OA[OpenAPI Spec<br/>Contrato]
        DOC[Documentación<br/>Guía de uso]
    end

    HC --> K8S[Kubernetes del Cliente]
    TF --> CLOUD[Nube del Cliente]
    DI --> REG[Registry]
    OA --> PORTAL[Developer Portal]
    DOC --> PORTAL
```

## 2. Catálogo de Aceleradores

### Aceleradores Core

| # | Acelerador | Qué resuelve | Complejidad |
|---|---|---|---|
| 1 | **Consent Manager** | Gestión completa del ciclo de vida del consentimiento | Alta |
| 2 | **Authorization Server** | Autenticación FAPI 2.0, emisión de tokens, SCA | Alta |
| 3 | **Payment Initiation** | Iniciar pagos bajo consentimiento | Media |
| 4 | **Account Aggregation** | Consolidar información financiera multi-banco | Media |
| 5 | **Directory Service** | Registro de entidades y ruteo | Baja |

### Aceleradores de Infraestructura

| # | Acelerador | Qué resuelve |
|---|---|---|
| 6 | **API Gateway Config** | Ingress + mTLS + rate limiting + routing |
| 7 | **Observability Stack** | Prometheus + Grafana + Loki + alertas |
| 8 | **Security Baseline** | Vault + cert-manager + Network Policies |
| 9 | **CI/CD Templates** | Pipelines para build y deploy |
| 10 | **Developer Portal** | Portal estático desplegable |

---

## 3. Arquitectura del Portal de Demostración

Lo que mantenemos nosotros para que el cliente vea y pruebe:

```mermaid
graph TB
    subgraph "Portal Público - openfinance-accelerators.com"
        subgraph "Secciones del Portal"
            LAND[Landing<br/>¿Qué es Open Finance?<br/>¿Qué son los aceleradores?]
            CAT[Catálogo<br/>Lista de aceleradores<br/>con descripción y pricing]
            DEMO[Demo Interactiva<br/>Videos + flujos animados<br/>de cada acelerador]
            DOCS[Documentación<br/>OpenAPI specs<br/>Guías de integración]
            SAND[Sandbox<br/>Probar APIs en vivo<br/>con datos mock]
            REG[Registro<br/>Crear cuenta<br/>Obtener API keys de prueba]
        end
    end

    subgraph "Sandbox Backend (1 nube)"
        SB_GW[API Gateway]
        SB_CM[Consent Manager<br/>modo demo]
        SB_AS[Auth Server<br/>modo demo]
        SB_PI[Payment API<br/>datos mock]
        SB_AA[Accounts API<br/>datos mock]
        SB_DB[(DB con datos ficticios)]
    end

    SAND --> SB_GW
    SB_GW --> SB_CM
    SB_GW --> SB_AS
    SB_GW --> SB_PI
    SB_GW --> SB_AA
    SB_CM --> SB_DB
    SB_AS --> SB_DB
```

### Experiencia del Cliente en el Portal

```mermaid
journey
    title Viaje del Cliente en el Portal
    section Descubrimiento
        Llega al portal: 5: Cliente
        Lee qué es Open Finance: 4: Cliente
        Ve catálogo de aceleradores: 5: Cliente
    section Exploración
        Lee documentación técnica: 4: Cliente
        Ve demos interactivas: 5: Cliente
        Crea cuenta de prueba: 3: Cliente
    section Validación
        Obtiene API keys sandbox: 4: Cliente
        Prueba flujo de consentimiento: 5: Cliente
        Prueba iniciación de pagos: 5: Cliente
        Valida que cumple su necesidad: 5: Cliente
    section Compra
        Solicita cotización: 4: Cliente
        Recibe propuesta: 4: Ventas
        Firma contrato: 3: Cliente
    section Implementación
        Recibe aceleradores: 5: Cliente
        Despliega en su infra: 3: Cliente
        Soporte de implementación: 5: Equipo
```

---

## 4. ¿Qué ve el cliente en el Portal?

### 4.1 Landing Page
- Explicación visual de Open Finance en Colombia
- Decreto 0368 y qué implica para las entidades
- Cómo los aceleradores resuelven el cumplimiento
- Casos de uso: pagos, agregación, consentimiento

### 4.2 Catálogo de Aceleradores
- Card por cada acelerador con:
  - Nombre y descripción
  - Qué problema resuelve
  - Diagrama de arquitectura
  - Tecnologías usadas
  - Nubes soportadas (AWS ✓ Azure ✓ GCP ✓)
  - Nivel de complejidad
  - Dependencias

### 4.3 Documentación Interactiva
- OpenAPI specs renderizadas (Swagger UI / Redoc)
- Ejemplos de request/response
- Flujos paso a paso con diagramas
- Guías de despliegue por nube
- Requisitos de infraestructura

### 4.4 Sandbox
- Ambiente real corriendo con datos mock
- El cliente puede:
  - Crear un consentimiento de prueba
  - Obtener tokens OAuth2/FAPI
  - Iniciar un pago ficticio
  - Consultar cuentas mock
  - Ver logs de auditoría
- Todo con credenciales temporales

---

## 5. Infraestructura Necesaria (Propuesta LITE)

### Lo que mantenemos nosotros

```mermaid
graph TB
    subgraph "Infraestructura Propia - 1 Nube (AWS)"
        subgraph "Portal Estático"
            CF[CloudFront CDN]
            S3[S3 - Assets estáticos]
        end

        subgraph "Sandbox"
            EKS_SB[EKS - 3 nodos<br/>4vCPU / 16GB cada uno]
            RDS_SB[(RDS PostgreSQL<br/>db.t3.medium)]
            REDIS_SB[(ElastiCache Redis<br/>cache.t3.small)]
        end

        subgraph "CI/CD"
            ECR[ECR - Imágenes]
            GHA[GitHub Actions]
        end

        subgraph "Seguridad"
            ACM[ACM - Certificados]
            R53[Route53 - DNS]
        end
    end
```

### Costo mensual detallado

| Recurso | Spec | Costo USD/mes |
|---|---|---|
| EKS Cluster (control plane) | Managed | $73 |
| EC2 Nodes (3x m5.xlarge) | 4vCPU/16GB x 3 | $420 |
| RDS PostgreSQL | db.t3.medium, 100GB, Multi-AZ | $180 |
| ElastiCache Redis | cache.t3.small, 2 nodos | $70 |
| ALB | Application Load Balancer | $30 |
| CloudFront | CDN para portal | $20 |
| S3 | Storage portal + logs | $10 |
| ECR | Container images | $15 |
| Route53 | DNS | $5 |
| ACM | Certificados SSL | $0 (gratis en AWS) |
| NAT Gateway | Salida a internet | $45 |
| Data Transfer | ~500GB/mes | $45 |
| GitHub Actions | CI/CD minutes | $50 |
| **TOTAL** | | **~$963/mes** |

### Costo anual: ~$11,556 USD

---

## 6. Modelo de Negocio

### Pricing sugerido por acelerador

| Paquete | Incluye | Precio sugerido |
|---|---|---|
| **Starter** | 1 acelerador + soporte básico | $30K - $50K/año |
| **Professional** | 3 aceleradores + soporte prioritario | $80K - $120K/año |
| **Enterprise** | Todos los aceleradores + soporte 24/7 + customización | $150K - $250K/año |
| **Implementación** | Despliegue en infra del cliente | $40K - $80K one-time |
| **Soporte adicional** | Horas de consultoría | $150 - $250/hora |

### Proyección de Revenue

| Clientes | Paquete promedio | Revenue anual |
|---|---|---|
| 1 cliente Enterprise | $200K | $200K |
| 3 clientes Professional | $100K c/u | $300K |
| 5 clientes Starter | $40K c/u | $200K |
| **Total con 9 clientes** | | **$700K** |
| **Costo de infra propia** | | **-$12K** |
| **Margen bruto** | | **$688K (~98%)** |

---

## 7. Roadmap de Implementación

```mermaid
gantt
    title Roadmap - Propuesta LITE
    dateFormat  YYYY-MM-DD
    section Fase 1 - Portal
    Diseño UI/UX del portal           :a1, 2026-06-01, 15d
    Desarrollo portal estático        :a2, after a1, 20d
    Contenido y documentación         :a3, after a1, 25d
    Deploy portal en CDN              :a4, after a2, 5d

    section Fase 2 - Aceleradores Core
    Consent Manager (Helm + Docker)   :b1, 2026-06-15, 40d
    Authorization Server FAPI 2.0     :b2, 2026-06-15, 45d
    API Gateway Config                :b3, 2026-07-01, 20d

    section Fase 3 - APIs de Negocio
    Payment Initiation API            :c1, 2026-08-01, 30d
    Account Aggregation API           :c2, 2026-08-01, 30d
    Directory Service                 :c3, 2026-08-15, 20d

    section Fase 4 - Sandbox
    Infraestructura sandbox (Terraform) :d1, 2026-07-15, 15d
    Deploy servicios en sandbox       :d2, after d1, 10d
    Datos mock y configuración        :d3, after d2, 10d
    Integración portal ↔ sandbox      :d4, after d3, 10d

    section Fase 5 - IaC Multi-nube
    Terraform modules AWS             :e1, 2026-09-01, 20d
    Terraform modules Azure           :e2, after e1, 20d
    Terraform modules GCP             :e3, after e2, 20d
    Testing multi-nube                :e4, after e3, 15d

    section Fase 6 - Go to Market
    Documentación de despliegue       :f1, 2026-10-15, 15d
    Guías por nube                    :f2, after f1, 10d
    Demo day con primer cliente       :f3, after f2, 5d
```

### Timeline estimado: ~5 meses hasta primer cliente

---

## 8. Ventajas de esta Propuesta

| Ventaja | Detalle |
|---|---|
| **Bajo costo operativo** | Solo ~$1K/mes de infra propia |
| **Alto margen** | El cliente paga la infra, nosotros vendemos software |
| **Escalable** | Cada nuevo cliente = revenue sin costo adicional de infra |
| **Multi-nube real** | Los aceleradores corren en cualquier K8s |
| **Rápido time to market** | Portal + sandbox en 2-3 meses |
| **Bajo riesgo** | No se invierte en infra pesada hasta tener clientes |
| **Demostrable** | El sandbox permite que el cliente pruebe antes de comprar |
