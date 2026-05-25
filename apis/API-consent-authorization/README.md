# API-consent-authorization

## Descripción

API para la integración entre el Authorization Server y el Consent Manager. Gestiona la autorización, rechazo y validación de consentimientos durante el flujo OAuth2/FAPI 2.0.

## Responsabilidad

- Validar consentimiento para pantalla de autorización
- Señalizar autorización del usuario (consent → AUTHORIZED)
- Señalizar rechazo del usuario (consent → REJECTED)
- Verificar vigencia de un consentimiento (para Resource Servers)
- Certificate binding validation

## Endpoints

| Método | Endpoint | Descripción |
|---|---|---|
| GET | `/v1/consents/{consentId}/validate` | Validar consent para auth flow |
| POST | `/v1/consents/{consentId}/authorize` | Usuario autorizó |
| POST | `/v1/consents/{consentId}/reject` | Usuario rechazó |
| GET | `/v1/consents/{consentId}/active` | Verificar vigencia (gateway) |
| POST | `/v1/consents/{consentId}/consume` | Marcar como consumido |

## Consumidores

- Authorization Server (durante flujo OAuth)
- API Gateway (validación en cada request)

## Seguridad

- Solo accesible desde red interna (service mesh)
- mTLS pod-to-pod (Istio)
- No expuesto a TPPs directamente
