# Análisis de Costos — Consent Manager

## Infraestructura AWS (Serverless-first, costo-eficiente)

### Ambiente Dev (~$180/mes)

| Recurso | Configuración | Costo/mes |
|---|---|---|
| EKS Control Plane | 1 cluster | $73 |
| EKS Nodes | 2x t3.medium Spot | $30 |
| Aurora Serverless v2 | 0.5 ACU min | $45 |
| ElastiCache Serverless | 2GB max | $10 |
| MSK Serverless | Low throughput | $15 |
| NAT Gateway | 1 (single AZ) | $35 |
| Secrets Manager | 5 secrets | $3 |
| **Total Dev** | | **~$211** |

### Ambiente Production (~$650/mes)

| Recurso | Configuración | Costo/mes |
|---|---|---|
| EKS Control Plane | 1 cluster | $73 |
| EKS Nodes | 3x m6i.large On-Demand | $280 |
| Aurora Serverless v2 | 1-16 ACU + reader | $120 |
| ElastiCache Serverless | 10GB max | $40 |
| MSK Serverless | Medium throughput | $50 |
| NAT Gateway | 3 (multi-AZ) | $105 |
| WAF | Standard rules | $20 |
| Secrets Manager | 10 secrets | $5 |
| CloudWatch | Logs + alarms | $30 |
| **Total Prod** | | **~$723** |

### Total mensual

| Ambiente | Costo |
|---|---|
| Dev | ~$211 |
| Production | ~$723 |
| **Total** | **~$934/mes** |

## Estrategia de Optimización

| Principio | Implementación | Ahorro |
|---|---|---|
| Serverless DB | Aurora Serverless v2 (escala de 0.5 a 16 ACU) | 70% vs RDS fijo |
| Serverless Cache | ElastiCache Serverless (paga por uso) | 85% vs nodo fijo |
| Serverless Kafka | MSK Serverless (paga por throughput) | 80% vs cluster fijo |
| Spot en dev | EKS nodes Spot instances | 60-90% en dev |
| Single NAT en dev | 1 NAT Gateway en vez de multi-AZ | $70/mes ahorro |

## Capacidad

| Métrica | Dev | Prod |
|---|---|---|
| Requests/seg | 100 | 5,000 |
| Consentimientos activos | 1K | 500K |
| Pods por servicio | 1-2 | 3-10 |
| Autoscaling | HPA + Cluster Autoscaler | Automático |
