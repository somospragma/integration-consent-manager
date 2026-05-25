# Developer Portal — Consent Manager Pragma

## Descripción

Portal de desarrollador gratuito y open source para exponer la documentación de las APIs del Consent Manager. Se despliega en **GitHub Pages** (gratis) usando **Redoc** (open source) para renderizar los OpenAPI specs de forma interactiva.

## Stack (100% gratuito)

| Componente | Herramienta | Costo |
|---|---|---|
| Documentación API | Redoc (open source) | $0 |
| Sitio estático | GitHub Pages | $0 |
| CI/CD | GitHub Actions | $0 (repos públicos) |
| Dominio custom | Opcional (Route53) | ~$1/mes |

## Estructura

```
developer-portal/
├── README.md
├── index.html                    # Landing page del portal
├── apis/
│   ├── consent-lifecycle.html    # Docs API Consent Lifecycle
│   ├── consent-query.html        # Docs API Consent Query
│   ├── consent-admin.html        # Docs API Admin
│   └── consent-authorization.html
├── specs/                        # OpenAPI specs (symlinks o copias)
│   ├── consent-lifecycle.yaml
│   ├── consent-query.yaml
│   ├── consent-admin.yaml
│   └── consent-authorization.yaml
├── guides/
│   ├── getting-started.md        # Guía de inicio rápido
│   ├── authentication.md         # Cómo autenticarse
│   ├── consent-flow.md           # Flujo de consentimiento
│   └── sandbox.md                # Cómo usar el sandbox
└── .github/
    └── workflows/
        └── deploy-portal.yml     # GitHub Actions para deploy
```

## Cómo funciona

1. Los OpenAPI specs (`openapi.yaml`) de cada API se copian a `specs/`
2. Redoc renderiza cada spec en una página HTML interactiva
3. GitHub Actions despliega automáticamente a GitHub Pages en cada push
4. El portal queda disponible en: `https://pragma.github.io/consent-manager-pragma/`

## Deploy local (para desarrollo)

```bash
cd developer-portal
npx http-server . -p 8000
# Abrir http://localhost:8000
```

## Deploy a GitHub Pages

```bash
# Se despliega automáticamente con cada push a main
# O manualmente:
git push origin main
# GitHub Actions ejecuta el workflow y publica en gh-pages
```
