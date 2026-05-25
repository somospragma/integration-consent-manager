# ADR-004: Developer Portal como Sitio Estático con Sandbox

## Estado
Propuesto

## Contexto
Las entidades financieras que quieran consumir los aceleradores de Open Finance necesitan un lugar donde:
- Entender qué es Open Finance y qué APIs están disponibles
- Leer documentación técnica interactiva
- Probar las APIs en un ambiente seguro (sandbox)
- Registrarse y obtener credenciales
- Monitorear su consumo

## Decisión
Construir el Developer Portal como un **sitio estático** (JAMstack) con un **sandbox backend aislado**.

### Arquitectura del Portal

**Frontend (Estático):**
- Framework: Astro o Next.js (static export)
- Hosting: CDN + Object Storage (CloudFront+S3 / Azure CDN+Blob / Cloud CDN+GCS)
- Contenido:
  - Landing explicativa de Open Finance
  - Catálogo de aceleradores (APIs)
  - Documentación OpenAPI interactiva (Swagger UI / Redoc)
  - Guías de integración paso a paso
  - Consola de desarrollador (API keys, métricas)

**Sandbox (Backend aislado):**
- Cluster K8s separado o namespace aislado
- Datos mock (no datos reales)
- Mismos contratos de API que producción
- Rate limiting más permisivo para pruebas
- Tokens de prueba con expiración corta

## Alternativas Consideradas

| Opción | Pros | Contras |
|---|---|---|
| Portal dinámico (SSR completo) | Más flexible | Más infra, más costo, más complejidad |
| Solo documentación PDF | Barato | No interactivo, mala experiencia |
| Postman Collections | Familiar para devs | No es un portal, no tiene onboarding |
| **Estático + Sandbox** | Rápido, barato, escalable, buena UX | Requiere backend para sandbox y auth |

## Consecuencias
- Costo de hosting muy bajo (solo CDN + storage)
- Excelente performance (estático = rápido)
- El sandbox requiere infraestructura dedicada pero aislada
- Se puede iterar rápido en contenido sin redeploy de backend
- Las entidades pueden probar antes de integrar en producción
