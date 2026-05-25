# API-consent-admin

## Descripción

API de administración para operadores y oficiales de la entidad financiera. Permite gestión avanzada, búsqueda sin restricciones, operaciones bulk y configuración.

## Responsabilidad

- Búsqueda avanzada sin restricción de TPP/usuario
- Revocación administrativa (ej: TPP suspendido)
- Operaciones bulk (revocar todos los consents de un TPP)
- Configuración de TTLs y políticas
- Métricas y reportes
- Health check del servicio

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/v1/admin/consents/search` | Búsqueda avanzada |
| DELETE | `/v1/admin/consents/{consentId}` | Revocación administrativa |
| POST | `/v1/admin/consents/bulk-revoke` | Revocación masiva |
| GET | `/v1/admin/metrics` | Métricas del consent manager |
| GET | `/v1/admin/config` | Obtener configuración actual |
| PUT | `/v1/admin/config` | Actualizar configuración |
| GET | `/v1/admin/health` | Health check |
| GET | `/v1/admin/health/ready` | Readiness probe |
| GET | `/v1/admin/health/live` | Liveness probe |

## Seguridad

- Solo accesible desde red interna
- Requiere rol ADMIN o CUSTOMER_CARE_OFFICER
- Audit log de cada operación administrativa
- No expuesto a TPPs
