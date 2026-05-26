# ADR-004: Developer Portal con Swagger UI en GitHub Pages

## Estado
Aceptado

## Contexto
Las entidades financieras que quieran consumir las APIs del Consent Manager necesitan un portal donde ver la documentación interactiva y probar los endpoints.

## Decisión
Usar **Swagger UI** como portal interactivo desplegado en **GitHub Pages** (carpeta `/docs`).

### Implementación
- `docs/index.html` — Portal con Swagger UI embebido
- `docs/specs/*.yaml` — OpenAPI specs de cada API
- GitHub Pages sirve desde `/docs` en rama `main`
- Colores de marca Pragma (#6429CD, #330072, #1D1D1B)
- Try it out habilitado para probar contra localhost o sandbox

### Funcionalidades
- Landing page con propuesta de valor del Consent Manager
- Tabs para navegar entre las 9 APIs
- Swagger UI con "Try it out" funcional
- API Key se inyecta automáticamente
- Diagrama de arquitectura con Mermaid JS
- Ecosistema de proveedores integrados

## Alternativas Consideradas

| Opción | Pros | Contras |
|---|---|---|
| Redoc | Bonito, 3 columnas | No tiene "Try it out" |
| Markdown puro | Visible en GitHub | No interactivo |
| Postman Published | Familiar | Requiere cuenta, no customizable |
| **Swagger UI + GitHub Pages** | Interactivo, gratis, customizable | Requiere repo público |

## Consecuencias
- El repo debe ser público para que GitHub Pages funcione (gratis)
- Los specs del portal son copias de los de `apis/` (se sincronizan manualmente)
- El portal es estático, no requiere backend
