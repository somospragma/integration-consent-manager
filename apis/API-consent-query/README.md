# API-consent-query

## Descripción

API de consulta y búsqueda de consentimientos. Permite a TPPs, usuarios y administradores buscar, filtrar y paginar consentimientos.

## Responsabilidad

- Listar consentimientos por usuario
- Listar consentimientos por TPP
- Búsqueda con filtros (estado, tipo, fecha)
- Historial de cambios de un consentimiento
- Paginación y ordenamiento

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/v1/consents` | Listar con filtros |
| GET | `/v1/consents/{consentId}/history` | Historial de estados |
| GET | `/v1/users/{userId}/consents` | Consentimientos de un usuario |
| GET | `/v1/tpp/{tppId}/consents` | Consentimientos de un TPP |

## Parámetros de Query

| Parámetro | Tipo | Descripción |
|---|---|---|
| `status` | enum | Filtrar por estado |
| `type` | enum | Filtrar por tipo |
| `fromDate` | date | Desde fecha |
| `toDate` | date | Hasta fecha |
| `page` | int | Número de página |
| `pageSize` | int | Tamaño de página (max 100) |
| `sort` | string | Campo de ordenamiento |

## Dependencias

- MS-consent-engine (datos)
- MS-audit-trail (historial)

## Seguridad

- mTLS + Bearer Token
- TPPs solo ven sus propios consentimientos
- Usuarios ven sus propios consentimientos
- Admins ven todos (vía API-consent-admin)
