# Comparativa de Proveedores de Consent Management

## Resumen Ejecutivo

Se analizaron 5 proveedores de consent management para identificar servicios comunes y capacidades que debe tener nuestro acelerador. Los proveedores se dividen en dos categorías:

- **Open Finance nativos:** Ozone, Sensedia — diseñados específicamente para ecosistemas de banca abierta
- **Privacy/Compliance generales:** Immuta, Didomi, Osano — enfocados en GDPR/CCPA y gobernanza de datos

Para nuestro caso (Open Finance Colombia, Decreto 0368), los proveedores más relevantes como referencia son **Ozone** y **Sensedia**, pero tomamos capacidades de todos.

---

## 1. Ozone API — Consent Manager

**Fuente:** [ozoneapi.com](https://ozoneapi.com) | [Open Finance UAE Docs](https://openfinanceuae.atlassian.net)

### Servicios principales

| Servicio | Descripción |
|---|---|
| Consent Lodging | Registro de consentimientos no autorizados ("intents") enviados por TPP vía Pushed Authorization Request (PAR) |
| Consent Lifecycle Management | Gestión de estados: Created → AwaitingAuthorization → Authorized → Consumed → Revoked → Expired |
| Consent Enrichment | Enriquecimiento con metadata del TPP o de la entidad financiera (LFI) |
| Payment State Management | Gestión de estados de pagos asociados a consentimientos |
| Consent Authorization Signalling | Señalización durante el flujo de Authorization Code |
| Consent History | Historial completo de cambios de estado por consentimiento |
| Consent Search & Filtering | Búsqueda avanzada con filtros (por TPP, usuario, tipo, estado, fecha) |
| Consent Dashboard (CMI) | Interfaz de gestión para el usuario final y para oficiales del banco |

### Características clave
- Integrado con Authorization Server (FAPI 2.0)
- Soporta múltiples tipos de consentimiento: accounts, payments, funds-confirmation
- Webhook/callback cuando cambia el estado de un consentimiento
- API RESTful con OpenAPI spec

---

## 2. Sensedia — Consent Engine

**Fuente:** [docs.sensedia.com](https://docs.sensedia.com) | [AWS Marketplace](https://aws.amazon.com/marketplace)

### Servicios principales

| Servicio | Descripción |
|---|---|
| Consent Creation | Creación de consentimientos con scope, permisos y expiración |
| Consent Authorization | Flujo de autorización integrado con Authorization Server |
| Consent Revocation | Revocación por usuario o por la entidad |
| Consent Querying | Consulta de consentimientos activos por usuario/TPP |
| Consent Expiration | Gestión automática de expiración temporal |
| Admin Portal | Portal de administración para gestión de consentimientos |
| TPP Registry Integration | Integración con directorio central de participantes |
| Metrics & Monitoring | Métricas de consentimientos (creados, revocados, expirados) |
| Sandbox Mode | Modo sandbox para pruebas de terceros |

### Características clave
- Diseñado para Open Finance Brasil (regulación del Banco Central)
- Integración nativa con API Gateway de Sensedia
- Soporte para Open Insurance además de Open Finance
- Certificación FAPI 2.0
- Disponible en AWS Marketplace como servicio managed

---

## 3. Immuta — Data Consent & Governance

**Fuente:** [immuta.com](https://www.immuta.com) | [documentation.immuta.com](https://documentation.immuta.com)

### Servicios principales

| Servicio | Descripción |
|---|---|
| Policy Engine | Motor de políticas en lenguaje natural para control de acceso |
| Access Request Workflows | Flujos de solicitud y aprobación de acceso a datos |
| Multi-Approver Workflows | Aprobación por múltiples stakeholders en paralelo |
| Data Masking | Enmascaramiento dinámico de datos sensibles según políticas |
| Audit Trail | Registro completo de quién accedió a qué datos y cuándo |
| Purpose-Based Access | Acceso basado en propósito declarado |
| Time-Bound Access | Acceso temporal con expiración automática |
| Data Marketplace | Catálogo de datos disponibles con solicitud de acceso |
| Guardrail Policies | Políticas de protección automáticas |

### Características clave
- Enfoque en gobernanza de datos a nivel de plataforma
- Políticas declarativas (no código)
- Integración con Snowflake, Databricks, AWS, Azure
- No es específico de Open Finance pero sus patrones son aplicables

---

## 4. Didomi — Consent Management Platform

**Fuente:** [developers.didomi.io](https://developers.didomi.io)

### Servicios principales

| Servicio | Descripción |
|---|---|
| Consent Collection | Captura de consentimiento via widgets/SDK (web, mobile) |
| Consent Events | Registro de eventos de consentimiento con timestamp |
| Consent Status API | Consulta del estado actual de consentimiento por usuario |
| User Preferences | Preferencias granulares dentro de un consentimiento |
| Purpose Management | Gestión de propósitos (para qué se usan los datos) |
| Vendor Management | Gestión de terceros con quienes se comparten datos |
| Cross-Platform ID Resolution | Resolución de identidad entre plataformas |
| Privacy Requests (DSAR) | Solicitudes de derechos del titular (acceso, eliminación, portabilidad) |
| Consent Proof | Prueba verificable de que el consentimiento fue otorgado |
| Regulation Compliance | Adaptación automática por regulación (GDPR, CCPA, LGPD) |
| Webhooks | Notificaciones cuando cambia un consentimiento |
| Analytics | Dashboard de tasas de consentimiento y tendencias |

### Características clave
- API RESTful con documentación completa
- SDK para web y mobile
- Multi-regulación (se adapta según país)
- Metadata personalizable por organización
- Integración con Google Consent Mode

---

## 5. Osano — Unified Consent & Preference Hub

**Fuente:** [osano.com](https://www.osano.com)

### Servicios principales

| Servicio | Descripción |
|---|---|
| Cookie Consent | Gestión de consentimiento de cookies |
| Unified Consent Hub | Hub centralizado de todos los tipos de consentimiento |
| Preference Center | Centro de preferencias del usuario |
| DSAR Automation | Automatización de solicitudes de derechos del titular |
| Vendor Risk Assessment | Evaluación de riesgo de privacidad de terceros |
| Consent Records | Registro inmutable de consentimientos otorgados |
| Multi-Language Support | Soporte para 45+ idiomas |
| Compliance Monitoring | Monitoreo continuo de cumplimiento |
| Data Mapping | Mapeo de datos personales en la organización |
| Privacy Assessments | Evaluaciones de impacto de privacidad (RoPAs) |

### Características clave
- Certificado por Google como CMP
- Enfoque en simplificar compliance para empresas
- Migración desde OneTrust y otras herramientas legacy
- Soporte para 50+ países y regulaciones

---

## Matriz Comparativa de Servicios

### Servicios COMUNES (presentes en 3+ proveedores)

| Servicio | Ozone | Sensedia | Immuta | Didomi | Osano | **Prioridad** |
|---|:---:|:---:|:---:|:---:|:---:|:---:|
| Crear consentimiento | ✅ | ✅ | ✅ | ✅ | ✅ | **CRÍTICO** |
| Consultar consentimiento | ✅ | ✅ | ✅ | ✅ | ✅ | **CRÍTICO** |
| Revocar consentimiento | ✅ | ✅ | ✅ | ✅ | ✅ | **CRÍTICO** |
| Ciclo de vida (estados) | ✅ | ✅ | ✅ | ✅ | ✅ | **CRÍTICO** |
| Auditoría/historial | ✅ | ✅ | ✅ | ✅ | ✅ | **CRÍTICO** |
| Expiración automática | ✅ | ✅ | ✅ | ✅ | ✅ | **CRÍTICO** |
| Gestión de propósitos | ✅ | ✅ | ✅ | ✅ | ✅ | **ALTO** |
| Gestión de permisos/scope | ✅ | ✅ | ✅ | ✅ | ✅ | **ALTO** |
| Webhooks/notificaciones | ✅ | ✅ | — | ✅ | — | **ALTO** |
| Búsqueda y filtrado | ✅ | ✅ | ✅ | ✅ | ✅ | **ALTO** |
| Prueba de consentimiento | ✅ | ✅ | ✅ | ✅ | ✅ | **ALTO** |
| Multi-regulación | — | ✅ | — | ✅ | ✅ | **MEDIO** |
| Preferencias granulares | — | — | — | ✅ | ✅ | **MEDIO** |
| DSAR (derechos del titular) | — | — | — | ✅ | ✅ | **MEDIO** |
| Analytics/métricas | — | ✅ | — | ✅ | ✅ | **MEDIO** |

### Servicios ESPECÍFICOS de Open Finance (Ozone + Sensedia)

| Servicio | Relevancia para nosotros |
|---|---|
| Pushed Authorization Request (PAR) | **CRÍTICO** — requerido por FAPI 2.0 |
| Integración con Authorization Server | **CRÍTICO** — flujo OAuth2/FAPI |
| Tipos de consentimiento (accounts, payments, funds) | **CRÍTICO** — Open Finance |
| Payment state management | **ALTO** — para API de pagos |
| TPP registry integration | **ALTO** — directorio de participantes |
| Consent enrichment (metadata) | **MEDIO** — enriquecer con info del TPP |
| Sandbox mode | **ALTO** — para el developer portal |

---

## Conclusiones

### Lo que DEBE tener nuestro Consent Manager (MVP)

1. **CRUD completo de consentimientos** — crear, leer, actualizar estado, eliminar
2. **Máquina de estados** — Created → AwaitingAuth → Authorized → Consumed → Revoked → Expired
3. **Integración con Authorization Server** — flujo FAPI 2.0 / OAuth2
4. **Tipos de consentimiento** — accounts, payments, funds-confirmation
5. **Gestión de permisos/scope** — qué datos se comparten
6. **Expiración automática** — TTL configurable
7. **Revocación** — por usuario y por entidad
8. **Auditoría inmutable** — log de cada operación
9. **Webhooks** — notificar cambios de estado
10. **API RESTful** — con OpenAPI spec

### Lo que DEBERÍA tener (fase 2)

11. Interfaz de gestión para el usuario (CMI)
12. Portal de administración para la entidad
13. Analytics y métricas
14. DSAR (derechos del titular)
15. Multi-regulación (preparado para otros países)
16. Preferencias granulares
