# MS-permission-registry

## Descripción

Microservicio que gestiona el catálogo de permisos, propósitos y la validación de que un consentimiento tiene los permisos necesarios para acceder a un recurso.

## Responsabilidad

- Catálogo de permisos disponibles por tipo de consentimiento
- Catálogo de propósitos de uso de datos
- Validación de permisos (¿este consent puede acceder a /balances?)
- Mapeo endpoint → permiso requerido
- Gestión de clusters de permisos

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/v1/permissions` | Catálogo completo de permisos |
| GET | `/v1/permissions?type={consentType}` | Permisos por tipo |
| GET | `/v1/permissions/validate` | Validar permiso para recurso |
| GET | `/v1/purposes` | Catálogo de propósitos |
| POST | `/v1/purposes` | Crear nuevo propósito (admin) |

## Mapeo Endpoint → Permiso

| Endpoint del Resource Server | Permiso Requerido |
|---|---|
| `GET /accounts` | `READ_ACCOUNTS_BASIC` |
| `GET /accounts/{id}` | `READ_ACCOUNTS_DETAIL` |
| `GET /balances` | `READ_BALANCES` |
| `GET /transactions` | `READ_TRANSACTIONS_BASIC` |
| `POST /domestic-payments` | `INITIATE_PAYMENT` |
| `GET /beneficiaries` | `READ_BENEFICIARIES` |

## Catálogo de Permisos

### Tipo: ACCOUNTS
- READ_ACCOUNTS_BASIC
- READ_ACCOUNTS_DETAIL
- READ_BALANCES
- READ_TRANSACTIONS_BASIC
- READ_TRANSACTIONS_DETAIL
- READ_BENEFICIARIES
- READ_PRODUCTS
- READ_STANDING_ORDERS
- READ_DIRECT_DEBITS
- READ_SCHEDULED_PAYMENTS

### Tipo: PAYMENTS
- INITIATE_PAYMENT
- READ_PAYMENT_STATUS

### Tipo: FUNDS_CONFIRMATION
- CONFIRM_FUNDS

## Stack

- Java 21 + Spring Boot 3.x
- PostgreSQL (catálogo)
- Redis (cache de mapeos para validación rápida)
