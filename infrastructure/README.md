# Infraestructura — Consent Manager Pragma

## Filosofía: Costo-Eficiente y Agnóstica

Esta infraestructura sigue el principio de **no aprovisionar por aprovisionar**. Cada recurso existe porque el Consent Manager lo necesita, con el sizing mínimo viable que escala automáticamente según demanda.

### Estrategia de Optimización de Costos

| Principio | Implementación |
|---|---|
| **Right-sizing** | Instancias pequeñas con autoscaling, no sobredimensionar |
| **Serverless donde sea posible** | Aurora Serverless v2, ElastiCache Serverless |
| **Spot/Preemptible para dev** | Nodos spot en ambientes no-productivos |
| **Apagar lo que no se usa** | Schedules para apagar dev/sandbox fuera de horario |
| **Compartir recursos** | Un cluster K8s compartido, namespaces por servicio |
| **Reservas para prod** | Reserved Instances/Savings Plans solo en producción |

### Costo Estimado Mensual

| Ambiente | Costo estimado USD/mes |
|---|---|
| Dev (horario laboral) | ~$180 |
| Sandbox (24/7 mínimo) | ~$250 |
| Production (HA) | ~$650 |
| **Total** | **~$1,080/mes** |

---

## Estructura del Proyecto

```
infra/
├── README.md                    # Este archivo
├── Makefile                     # Comandos simplificados
├── modules/
│   ├── networking/              # VPC, subnets, security groups
│   ├── eks-cluster/             # EKS cluster (compartido)
│   ├── database/                # Aurora Serverless v2 PostgreSQL
│   ├── cache/                   # ElastiCache Serverless Redis
│   ├── messaging/               # MSK Serverless (Kafka)
│   ├── secrets/                 # Secrets Manager
│   └── observability/           # CloudWatch + Prometheus
├── environments/
│   ├── dev/                     # Desarrollo (costo mínimo)
│   │   ├── main.tf
│   │   ├── variables.tf
│   │   ├── outputs.tf
│   │   └── terraform.tfvars
│   ├── sandbox/                 # Sandbox para TPPs
│   │   └── ...
│   └── prod/                    # Producción (HA)
│       └── ...
└── scripts/
    ├── init-backend.sh          # Crear S3 bucket para state
    ├── deploy.sh                # Deploy automatizado
    └── destroy.sh               # Destruir ambiente
```

---

## Prerrequisitos

### Herramientas necesarias

```bash
# Terraform
brew install terraform    # >= 1.7.0

# AWS CLI
brew install awscli       # >= 2.x
aws configure             # Configurar credenciales

# kubectl (para verificar cluster)
brew install kubectl

# Helm (para deployments posteriores)
brew install helm
```

### Permisos AWS (IAM Policy mínima)

El usuario/rol que ejecuta Terraform necesita:
- `ec2:*` (VPC, Security Groups, Subnets)
- `eks:*` (Cluster EKS)
- `rds:*` (Aurora Serverless)
- `elasticache:*` (Redis)
- `kafka:*` (MSK)
- `secretsmanager:*` (Secrets)
- `iam:*` (Roles, Policies)
- `s3:*` (Terraform state)
- `dynamodb:*` (Terraform locks)

---

## Guía Paso a Paso

### 1. Inicializar el Backend (solo la primera vez)

```bash
cd infra/scripts
chmod +x init-backend.sh
./init-backend.sh
```

Esto crea:
- S3 bucket para Terraform state (versionado + cifrado)
- DynamoDB table para state locking

### 2. Desplegar un Ambiente

```bash
# Opción A: Usando Makefile
make plan ENV=dev
make apply ENV=dev

# Opción B: Manual
cd infra/environments/dev
terraform init
terraform plan -out=plan.out
terraform apply plan.out
```

### 3. Verificar el Despliegue

```bash
# Obtener kubeconfig
aws eks update-kubeconfig --name consent-manager-dev --region sa-east-1

# Verificar cluster
kubectl get nodes
kubectl get ns

# Verificar conectividad a DB
terraform output -raw database_endpoint
```

### 4. Desplegar los Microservicios (post-infra)

```bash
# Desde la raíz del proyecto
cd ../
helm install consent-manager ./helm/consent-manager \
  --namespace consent-manager-dev \
  --values helm/values-dev.yaml
```

### 5. Destruir un Ambiente

```bash
make destroy ENV=dev
# o
cd infra/environments/dev
terraform destroy
```

---

## Ambientes

| Ambiente | Propósito | Horario | HA |
|---|---|---|---|
| `dev` | Desarrollo del equipo | Lun-Vie 8am-8pm | No |
| `sandbox` | Pruebas de TPPs | 24/7 | No |
| `prod` | Producción | 24/7 | Sí (Multi-AZ) |

---

## Decisiones de Arquitectura (Costo-Eficiente)

### ¿Por qué Aurora Serverless v2 en vez de RDS estándar?
- Se escala automáticamente de 0.5 a 16 ACUs
- En dev con poco tráfico paga solo ~$0.12/ACU-hora
- No hay que elegir instance class ni pagar por capacidad ociosa

### ¿Por qué ElastiCache Serverless en vez de nodos fijos?
- Paga por GB almacenado + ECPUs consumidos
- En dev con poco uso: ~$5-10/mes vs ~$70/mes con nodo fijo
- Escala automáticamente en picos

### ¿Por qué un solo EKS cluster compartido?
- El control plane de EKS cuesta $73/mes fijo
- Un cluster con namespaces separados es más barato que múltiples clusters
- Network Policies aíslan los servicios

### ¿Por qué nodos Spot en dev?
- 60-90% más baratos que On-Demand
- Aceptable para dev (si se interrumpe, se re-schedula)
- Prod usa On-Demand con Reserved Instance discount
