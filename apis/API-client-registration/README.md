# API-client-registration

**BIAN Domain:** Party Authentication (Dynamic Client Registration)

## Descripción

API para registro dinámico de TPPs (Dynamic Client Registration - DCR). Permite a entidades terceras registrarse automáticamente presentando un Software Statement Assertion (SSA) firmado por el directorio central.

## Endpoints

| # | Operación | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 1 | Initiate | POST | `/register` | Registrar nuevo TPP |
| 2 | Retrieve | GET | `/register/{clientId}` | Obtener configuración del cliente |
| 3 | Update | PUT | `/register/{clientId}` | Actualizar configuración |
| 4 | Execute | DELETE | `/register/{clientId}` | Eliminar registro |

## Seguridad

- mTLS obligatorio
- SSA firmado por el directorio central (validar firma)
- Validar que el TPP está activo en el directorio
- Validar redirect_uris contra los registrados en directorio
- Validar roles (AISP, PISP) contra permisos solicitados

## Dependencias

- MS-authorization-server (gestión de clientes)
- Directory Service (validar SSA y estado del TPP)
