# API-permission-catalog

**BIAN Domain:** Customer Products and Services

## Descripción

API para consultar el catálogo de permisos disponibles y validar si un consentimiento tiene los permisos necesarios para acceder a un recurso específico. Usado por el API Gateway en cada request.

## Endpoints

| # | Operación | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 1 | Retrieve | GET | `/v1/permissions` | Catálogo completo de permisos |
| 2 | Retrieve | GET | `/v1/permissions?type={consentType}` | Permisos por tipo de consent |
| 3 | Execute | GET | `/v1/permissions/validate` | Validar permiso para endpoint |
| 4 | Retrieve | GET | `/v1/purposes` | Catálogo de propósitos |
| 5 | Initiate | POST | `/v1/purposes` | Crear propósito (admin) |

## Seguridad

- GET /permissions: público (catálogo informativo)
- GET /permissions/validate: interno (solo API Gateway)
- POST /purposes: requiere rol ADMIN

## Dependencias

- MS-permission-registry (lógica y persistencia)
