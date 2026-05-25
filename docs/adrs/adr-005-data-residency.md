# ADR-005: Residencia de Datos y Estrategia Multi-Región

## Estado
Propuesto

## Contexto
Los datos de consentimiento y transacciones financieras deben residir en Colombia (cumplimiento Ley 1581 de 2012). Sin embargo, la plataforma debe estar preparada para escalar a otros países.

## Decisión
Implementar un modelo de **residencia de datos por región** con capacidad de expansión.

### Diseño

**Región primaria — Colombia:**
- AWS: sa-east-1 (São Paulo) — más cercana disponible
- Azure: Brazil South
- GCP: southamerica-east1 (São Paulo)

> Nota: Ningún cloud provider tiene región en Colombia. Se usa São Paulo como la más cercana con baja latencia (~30ms). Si se requiere estricta residencia en Colombia, se evalúa colocation o edge.

**Modelo de datos:**
- Datos de consentimiento: siempre en región primaria
- Datos transaccionales: en región primaria, replicación async para DR
- Logs de auditoría: inmutables en región primaria + backup cross-region
- Datos del portal: CDN global (no sensibles)

**Preparación multi-país:**
- Esquema de base de datos con `country_code` / `region`
- Terraform modules parametrizados por región
- Capacidad de levantar un stack completo en otra región con un `terraform apply`
- Políticas de replicación configurables por tipo de dato

## Alternativas Consideradas

| Opción | Pros | Contras |
|---|---|---|
| Todo en una sola región fija | Simple | No escala internacionalmente |
| Multi-región activo-activo desde día 1 | Máxima disponibilidad | Costo 2-3x, complejidad extrema |
| **Región primaria + diseño extensible** | Balance costo/preparación | Requiere disciplina en el diseño |

## Consecuencias
- Cumplimiento con leyes de protección de datos colombianas
- Latencia aceptable desde Colombia (~30ms a São Paulo)
- Costo controlado (una sola región activa inicialmente)
- Expansión a otros países requiere solo configuración, no rediseño
- Se debe documentar claramente qué datos van en qué región
