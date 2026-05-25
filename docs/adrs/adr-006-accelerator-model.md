# ADR-006: Modelo de Aceleradores Desplegables

## Estado
Propuesto

## Contexto
Se necesita definir cómo se empaquetan y distribuyen los componentes de Open Finance a los clientes. Hay dos modelos posibles: operar la plataforma como SaaS o entregar aceleradores que el cliente despliega en su propia infraestructura.

## Decisión
Adoptar un **modelo de aceleradores desplegables** donde cada componente del ecosistema se empaqueta como:

1. **Helm Chart** — para despliegue en cualquier Kubernetes
2. **Terraform Module** — para provisionar la infraestructura necesaria
3. **Docker Image** — imagen del microservicio lista para correr
4. **OpenAPI Spec** — contrato de la API
5. **Guía de despliegue** — documentación por nube

### Catálogo de Aceleradores

| Acelerador | Descripción | Dependencias |
|---|---|---|
| `consent-manager` | Motor de consentimiento completo | PostgreSQL, Redis, Kafka |
| `auth-server` | Authorization Server FAPI 2.0 | PostgreSQL, Redis, Vault |
| `payment-initiation` | API de iniciación de pagos | consent-manager, auth-server |
| `account-aggregation` | API de agregación de cuentas | consent-manager, auth-server |
| `directory-service` | Directorio de entidades y ruteo | PostgreSQL |
| `api-gateway` | Configuración de Ingress + mTLS | Istio/Envoy |
| `observability-stack` | Prometheus + Grafana + Loki | — |
| `developer-portal` | Portal estático + sandbox config | — |

### Distribución

```
Cliente descarga/accede a:
├── helm-charts/
│   ├── consent-manager/
│   ├── auth-server/
│   ├── payment-initiation/
│   └── ...
├── terraform-modules/
│   ├── aws/
│   ├── azure/
│   └── gcp/
├── docs/
│   ├── deployment-guide-aws.md
│   ├── deployment-guide-azure.md
│   └── deployment-guide-gcp.md
└── openapi-specs/
    ├── consent-api.yaml
    ├── payment-api.yaml
    └── accounts-api.yaml
```

## Alternativas Consideradas

| Opción | Pros | Contras |
|---|---|---|
| SaaS multi-tenant | Revenue recurrente, control total | Costo alto de infra, responsabilidad operativa |
| Solo código fuente | Máxima flexibilidad para cliente | Sin estandarización, difícil soporte |
| **Aceleradores empaquetados** | Portable, estandarizado, fácil de soportar | Cliente necesita K8s expertise |

## Consecuencias
- Cada acelerador debe ser auto-contenido y bien documentado
- Se necesita versionamiento semántico estricto
- Los Helm charts deben ser configurables (values.yaml extenso)
- Se requiere un registry privado para distribuir imágenes
- El soporte se simplifica: "¿qué versión tienes? actualiza a X"
- Se puede cobrar por acelerador individual o como bundle
