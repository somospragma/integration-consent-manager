# Análisis de Costos — Plataforma Open Finance Multi-Nube

## 1. Propuesta FULL: Infraestructura Propia Multi-Nube (3 nubes)

### Escenario: Plataforma completa desplegada en AWS + Azure + GCP

#### Costo mensual estimado POR NUBE

| Componente | Especificación | Costo/mes USD |
|---|---|---|
| **Kubernetes Cluster** | 5 nodos x 8vCPU/32GB (prod) | $1,800 - $2,500 |
| **Kubernetes Cluster** | 3 nodos x 4vCPU/16GB (sandbox) | $600 - $900 |
| **Kubernetes Cluster** | 2 nodos x 4vCPU/16GB (dev/stg) | $400 - $600 |
| **Base de datos PostgreSQL** | HA, 4vCPU/16GB, 500GB storage | $800 - $1,200 |
| **Redis Cache** | 2 nodos, 6GB | $300 - $500 |
| **Load Balancer** | ALB/LB con WAF | $200 - $400 |
| **CDN + Storage** | Portal estático + assets | $50 - $150 |
| **Container Registry** | Imágenes Docker | $30 - $80 |
| **KMS / Vault** | Cifrado + secrets | $100 - $200 |
| **Networking** | NAT Gateway, DNS, transferencia | $200 - $500 |
| **Observabilidad** | Logs, métricas, tracing (storage) | $300 - $600 |
| **Kafka/Event Bus** | 3 brokers managed | $500 - $800 |
| **Backup & DR** | Snapshots, replicación | $200 - $400 |
| **Certificados & DNS** | mTLS, dominios | $50 - $100 |
| **TOTAL POR NUBE** | | **$5,530 - $8,930** |

#### Costo total mensual (3 nubes)

| Escenario | Costo mensual USD |
|---|---|
| Mínimo (3 nubes, sizing conservador) | **$16,590** |
| Medio (3 nubes, sizing recomendado) | **$22,000** |
| Máximo (3 nubes, alta disponibilidad) | **$26,790** |

#### Costo anual estimado

| Escenario | Costo anual USD |
|---|---|
| Mínimo | **$199,080** |
| Medio | **$264,000** |
| Máximo | **$321,480** |

### ¿Cuántos usuarios/entidades justifican este costo?

| Métrica | Mínimo para ser viable |
|---|---|
| Entidades consumidoras (TPPs) | 50+ entidades activas |
| Transacciones/mes | 5M+ transacciones |
| Usuarios finales con consentimientos | 500K+ usuarios |
| Revenue por transacción (estimado) | $0.01 - $0.05 USD |
| Revenue mensual necesario | $50K - $250K USD |

> **Conclusión:** Una plataforma FULL multi-nube en las 3 nubes simultáneamente solo se justifica con un volumen alto de entidades y transacciones, o como requisito regulatorio/contractual.

---

## 2. Propuesta LITE: Aceleradores sobre Infraestructura del Cliente

### Concepto

En lugar de mantener infraestructura propia en 3 nubes, se entregan **aceleradores desplegables** (Helm charts, Terraform modules, Docker images) que el cliente instala en SU propia infraestructura. Se mantiene solo un **portal de demostración + sandbox** para que el cliente vea cómo funciona antes de comprar.

### ¿Qué se entrega al cliente?

| Entregable | Descripción |
|---|---|
| **Helm Charts** | Paquetes listos para instalar en cualquier K8s |
| **Terraform Modules** | IaC para provisionar la infra necesaria en su nube |
| **Docker Images** | Imágenes de los microservicios (Consent Manager, Auth Server, APIs) |
| **OpenAPI Specs** | Contratos de API documentados |
| **Guías de despliegue** | Documentación paso a paso por nube |
| **Configuración de seguridad** | Templates de Istio, Network Policies, Vault config |

### ¿Qué infraestructura propia se mantiene?

Solo lo necesario para **demostrar y vender**:

| Componente | Especificación | Costo/mes USD |
|---|---|---|
| **Developer Portal** | Sitio estático en CDN | $50 - $100 |
| **Sandbox (1 nube)** | K8s pequeño (3 nodos 4vCPU/16GB) | $600 - $900 |
| **DB Sandbox** | PostgreSQL pequeño + Redis | $200 - $300 |
| **CI/CD** | Pipelines para build de imágenes | $100 - $200 |
| **Container Registry** | Para distribuir imágenes a clientes | $50 - $100 |
| **Dominio + SSL** | Portal y sandbox | $30 - $50 |
| **TOTAL LITE** | | **$1,030 - $1,650** |

#### Costo anual propuesta LITE

| Escenario | Costo anual USD |
|---|---|
| Mínimo | **$12,360** |
| Máximo | **$19,800** |

### Comparativa

| Aspecto | FULL (3 nubes) | LITE (aceleradores) |
|---|---|---|
| Costo mensual | $16K - $27K | $1K - $1.6K |
| Costo anual | $199K - $321K | $12K - $20K |
| Responsabilidad de infra | Propia | Del cliente |
| Time to market | 6-9 meses | 3-4 meses |
| Escalabilidad | Ilimitada (propia) | Depende del cliente |
| Modelo de negocio | SaaS / Plataforma | Licencia + Soporte |
| Complejidad operativa | Alta (3 nubes) | Baja (solo sandbox) |
| Experiencia del cliente | Inmediata (ya está corriendo) | Requiere despliegue |

---

## 3. Propuesta RECOMENDADA: Híbrida

### Concepto

Mantener un **portal + sandbox propio** para demostración y onboarding, pero el despliegue productivo se hace en la **infraestructura del cliente**. Es lo mejor de ambos mundos.

```
┌─────────────────────────────────────────────────────┐
│          INFRAESTRUCTURA PROPIA (LITE)              │
│                                                     │
│  ┌─────────────┐  ┌─────────────────────────┐     │
│  │  Developer  │  │  Sandbox                 │     │
│  │  Portal     │  │  (1 nube, datos mock)    │     │
│  │  (estático) │  │  Para demos y pruebas    │     │
│  └─────────────┘  └─────────────────────────┘     │
│                                                     │
│  Costo: ~$1,500/mes                                │
└─────────────────────────────────────────────────────┘
                         │
                         │ El cliente prueba en sandbox
                         │ Decide comprar
                         ▼
┌─────────────────────────────────────────────────────┐
│       INFRAESTRUCTURA DEL CLIENTE (PROD)            │
│                                                     │
│  Se despliegan los aceleradores:                    │
│  - Helm Charts → su K8s                            │
│  - Terraform Modules → su nube                     │
│  - Docker Images → su registry                     │
│                                                     │
│  Costo: lo asume el cliente                        │
│  Soporte: se cobra por licencia + soporte          │
└─────────────────────────────────────────────────────┘
```

### Costo mensual propuesta HÍBRIDA

| Componente | Costo/mes USD |
|---|---|
| Portal estático (CDN) | $80 |
| Sandbox K8s (1 nube, AWS o GCP) | $800 |
| DB + Cache sandbox | $250 |
| CI/CD + Registry | $150 |
| Monitoring sandbox | $100 |
| DNS + Certs | $40 |
| **TOTAL** | **~$1,420/mes** |
| **TOTAL ANUAL** | **~$17,040** |

### Modelo de Revenue (propuesta LITE/Híbrida)

| Concepto | Precio sugerido |
|---|---|
| Licencia de aceleradores (anual) | $50K - $150K por cliente |
| Soporte técnico (mensual) | $5K - $15K por cliente |
| Implementación/despliegue | $30K - $80K one-time |
| Capacitación | $10K - $20K |
| Customización | Por hora/sprint |

### Break-even

| Escenario | Clientes necesarios |
|---|---|
| Solo licencia ($50K/año) | 1 cliente cubre infra propia |
| Licencia + soporte | 1 cliente genera profit |
| 5 clientes | Revenue $250K - $750K/año |

---

## 4. Resumen Ejecutivo

| Propuesta | Costo mensual | Costo anual | Para quién |
|---|---|---|---|
| **FULL** (3 nubes propias) | $16K - $27K | $199K - $321K | Si se opera como SaaS con muchos clientes |
| **LITE** (solo aceleradores) | $1K - $1.6K | $12K - $20K | Si se vende como producto/licencia |
| **HÍBRIDA** (portal + sandbox propio, prod en cliente) | ~$1.4K | ~$17K | **Recomendada** — demuestra y vende, cliente opera |

### Recomendación

**Arrancar con la propuesta HÍBRIDA:**
1. Construir el Developer Portal + Sandbox en 1 nube (AWS o GCP)
2. Desarrollar los aceleradores como Helm Charts + Terraform Modules portables
3. El cliente ve el portal, prueba en sandbox, y luego despliega en su infra
4. Se cobra licencia + soporte + implementación
5. Si el volumen crece, se puede migrar a modelo FULL (SaaS)

Esto minimiza el riesgo financiero inicial y permite validar el producto antes de invertir en infraestructura multi-nube completa.
