# API Permission Catalog — Documentación Funcional

## Propósito

Expone el catálogo de permisos disponibles que un TPP puede solicitar al crear un consentimiento. También valida si un permiso es suficiente para acceder a un endpoint específico.

---

## GET /v1/permissions — Catálogo de permisos

### ¿Qué hace?

Retorna la lista completa de permisos disponibles en el ecosistema. No requiere autenticación (es información pública).

### Parámetros

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `type` | string | No | Filtrar por tipo de consentimiento |

### Response

| Campo | Tipo | Descripción |
|---|---|---|
| `data[].code` | string | Código del permiso (usar en POST /consents) |
| `data[].name` | string | Nombre legible |
| `data[].description` | string | Qué permite acceder |
| `data[].consentType` | string | Tipo de consent donde aplica |
| `data[].active` | boolean | Si está habilitado |

### Catálogo completo

| Código | Nombre | Tipo | Qué permite |
|---|---|---|---|
| `READ_ACCOUNTS_BASIC` | Lectura básica de cuentas | ACCOUNTS | Tipo, estado, nickname |
| `READ_ACCOUNTS_DETAIL` | Lectura detallada de cuentas | ACCOUNTS | + número de cuenta completo |
| `READ_BALANCES` | Lectura de saldos | ACCOUNTS | Saldo disponible y contable |
| `READ_TRANSACTIONS_BASIC` | Movimientos básicos | ACCOUNTS | Monto, fecha, tipo |
| `READ_TRANSACTIONS_DETAIL` | Movimientos detallados | ACCOUNTS | + contraparte, referencia, merchant |
| `READ_BENEFICIARIES` | Beneficiarios | ACCOUNTS | Cuentas destino registradas |
| `READ_PRODUCTS` | Productos financieros | ACCOUNTS | Tipo de producto, condiciones |
| `READ_STANDING_ORDERS` | Órdenes permanentes | ACCOUNTS | Pagos recurrentes activos |
| `INITIATE_PAYMENT` | Iniciar pago | PAYMENTS | Ejecutar una transferencia |
| `READ_PAYMENT_STATUS` | Estado del pago | PAYMENTS | Consultar si el pago se completó |
| `CONFIRM_FUNDS` | Confirmar fondos | FUNDS_CONFIRMATION | Verificar si hay saldo suficiente |

---

## GET /v1/permissions/validate — Validar permiso

### ¿Qué hace?

Verifica si un permiso específico es suficiente para acceder a un endpoint. Usado internamente por el API Gateway en cada request.

### Parámetros

| Parámetro | Tipo | Obligatorio | Descripción |
|---|---|---|---|
| `permission` | string | Sí | Código del permiso a validar |
| `httpMethod` | string | Sí | GET, POST, DELETE |
| `endpoint` | string | Sí | Endpoint solicitado (ej: /accounts/123/balances) |

### Response

| Campo | Tipo | Descripción |
|---|---|---|
| `valid` | boolean | Si el permiso es suficiente |
| `requestedPermission` | string | Permiso que se consultó |
| `requiredPermission` | string | Permiso que realmente se necesita |
| `endpoint` | string | Endpoint consultado |

### Mapeo endpoint → permiso requerido

| Endpoint | Método | Permiso requerido |
|---|---|---|
| `/accounts` | GET | READ_ACCOUNTS_BASIC |
| `/accounts/{id}` | GET | READ_ACCOUNTS_DETAIL |
| `/accounts/{id}/balances` | GET | READ_BALANCES |
| `/accounts/{id}/transactions` | GET | READ_TRANSACTIONS_BASIC |
| `/beneficiaries` | GET | READ_BENEFICIARIES |
| `/products` | GET | READ_PRODUCTS |
| `/standing-orders` | GET | READ_STANDING_ORDERS |
| `/domestic-payments` | POST | INITIATE_PAYMENT |
| `/domestic-payments/{id}` | GET | READ_PAYMENT_STATUS |
| `/funds-confirmation` | POST | CONFIRM_FUNDS |

---

## GET /v1/purposes — Catálogo de propósitos

### ¿Qué hace?

Retorna los propósitos de uso de datos disponibles. Un propósito describe PARA QUÉ se usan los datos.

### Propósitos estándar

| Código | Nombre | Descripción |
|---|---|---|
| `ACCOUNT_INFORMATION` | Información de cuentas | Consultar cuentas y saldos |
| `PAYMENT_INITIATION` | Iniciación de pagos | Ejecutar pagos en nombre del usuario |
| `FUNDS_CONFIRMATION` | Confirmación de fondos | Verificar disponibilidad |
| `FINANCIAL_AGGREGATION` | Agregación financiera | Consolidar info de múltiples bancos |
| `CREDIT_SCORING` | Evaluación crediticia | Análisis de riesgo crediticio |
| `PERSONAL_FINANCE` | Finanzas personales | Apps de gestión financiera |
