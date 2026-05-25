# API-audit

**BIAN Domain:** Regulatory Compliance

## Descripción

API para consulta de logs de auditoría inmutables. Permite a administradores y reguladores consultar el historial de operaciones sobre consentimientos con verificación de integridad.

## Endpoints

| # | Operación | Método | Endpoint | Descripción |
|---|---|---|---|---|
| 1 | Retrieve | GET | `/v1/audit?consentId={id}` | Logs de un consentimiento |
| 2 | Retrieve | GET | `/v1/audit?actorId={id}` | Logs de un actor |
| 3 | Retrieve | GET | `/v1/audit?from={d}&to={d}` | Logs por rango de fechas |
| 4 | Execute | GET | `/v1/audit/export` | Exportar para regulador |
| 5 | Execute | GET | `/v1/audit/integrity-check` | Verificar cadena de hashes |

## Seguridad

- Solo accesible desde red interna
- Requiere rol ADMIN, AUDITOR o REGULATOR
- Export requiere AUDITOR o REGULATOR
- Integrity check requiere ADMIN
- Datos PII enmascarados en respuestas

## Dependencias

- MS-audit-trail (persistencia y verificación)
