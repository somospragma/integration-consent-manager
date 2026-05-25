# ADR-002: Kubernetes como Capa de Cómputo Unificada

## Estado
Propuesto

## Contexto
Se necesita una plataforma de cómputo que sea consistente entre AWS, Azure y GCP, donde se ejecuten los microservicios del ecosistema Open Finance.

## Decisión
Usar **Kubernetes managed** (EKS/AKS/GKE) como capa de cómputo unificada con Docker como runtime de contenedores.

### Configuración base por cluster:
- **Node pools:** Mínimo 3 nodos (multi-AZ) en producción
- **Autoscaling:** HPA por servicio + Cluster Autoscaler
- **Service Mesh:** Istio para mTLS pod-to-pod y observabilidad
- **Ingress:** Envoy-based (Istio Gateway o NGINX Ingress)
- **GitOps:** ArgoCD para gestión declarativa de deployments

## Alternativas Consideradas

| Opción | Pros | Contras |
|---|---|---|
| Serverless (Lambda/Functions) | Sin gestión de infra | Vendor lock-in, cold starts, no portable |
| VMs con Docker Compose | Simple | No escalable, no portable, sin orquestación |
| Nomad | Ligero | Menor ecosistema, menos soporte managed |
| **Kubernetes managed** | Portable, maduro, ecosistema rico | Complejidad operativa, curva de aprendizaje |

## Consecuencias
- Consistencia total entre nubes a nivel de workloads
- El equipo debe tener expertise en Kubernetes
- Se pueden usar las mismas herramientas (Helm, Kustomize, ArgoCD) en todas las nubes
- Los manifiestos de K8s son 100% portables

## Sizing Inicial Recomendado

| Ambiente | Nodos | Tipo instancia (equiv.) | RAM total | CPU total |
|---|---|---|---|---|
| Dev | 2 | 4 vCPU / 16GB | 32 GB | 8 vCPU |
| Sandbox | 3 | 4 vCPU / 16GB | 48 GB | 12 vCPU |
| Staging | 3 | 8 vCPU / 32GB | 96 GB | 24 vCPU |
| Production | 5-10 | 8 vCPU / 32GB | 160-320 GB | 40-80 vCPU |
