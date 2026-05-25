# ADR-003: Seguridad basada en FAPI 2.0 y Zero Trust

## Estado
Propuesto

## Contexto
El ecosistema de Open Finance requiere cumplir con FAPI 2.0, OAuth 2.0, mTLS y estándares de la SFC Colombia. La seguridad no es un add-on sino un requisito regulatorio.

## Decisión
Implementar un modelo de seguridad **Zero Trust** con:

### Autenticación y Autorización
- **FAPI 2.0 Security Profile** como base del Authorization Server
- **OAuth 2.0** con grant types: authorization_code (PKCE) + client_credentials
- **private_key_jwt** para autenticación de clientes (no client_secret)
- **mTLS** obligatorio para todas las comunicaciones entre entidades
- **JWT firmado** (RS256/ES256) con claims de consentimiento

### Seguridad en Infraestructura
- **Istio Service Mesh** para mTLS automático pod-to-pod
- **Network Policies** deny-all por defecto
- **HashiCorp Vault** para gestión de secrets (portable multi-nube)
- **KMS por nube** para cifrado de datos en reposo
- **cert-manager** para rotación automática de certificados

### Auditoría
- Logs inmutables de cada operación
- Campos sensibles enmascarados/cifrados
- Retención según regulación (mínimo 5 años)

## Alternativas Consideradas

| Opción | Pros | Contras |
|---|---|---|
| OAuth 2.0 básico sin FAPI | Más simple | No cumple regulación |
| API Keys solamente | Muy simple | Inseguro para Open Finance |
| Secrets en K8s nativos | Sin dependencia extra | No portable, sin rotación automática |
| **FAPI 2.0 + Vault + Istio** | Cumple regulación, portable, zero trust | Complejidad alta |

## Consecuencias
- Cumplimiento total con Decreto 0368 y SFC
- Certificación FAPI 2.0 alcanzable
- Mayor complejidad operativa
- Requiere expertise en seguridad financiera
- Vault como single point of truth para secrets en todas las nubes
