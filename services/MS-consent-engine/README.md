# MS-consent-engine

## Descripción

Microservicio core que implementa la lógica de negocio del consentimiento. Gestiona la máquina de estados, validaciones de negocio, expiración y persistencia.

## Responsabilidad

- Máquina de estados del consentimiento
- Validaciones de negocio (permisos válidos, TTL, tipo)
- Persistencia en PostgreSQL
- Cache de consentimientos activos en Redis
- Expiración automática (scheduled job)
- Emisión de eventos de dominio (Kafka)

## Máquina de Estados

```
CREATED → AWAITING_AUTHORIZATION → AUTHORIZED → CONSUMED
                                  → REJECTED
                                  → REVOKED
                                  → EXPIRED
```

## Eventos Emitidos (Kafka)

| Evento | Topic | Cuándo |
|---|---|---|
| `consent.created` | `consent-events` | Nuevo consent |
| `consent.authorized` | `consent-events` | Usuario autorizó |
| `consent.rejected` | `consent-events` | Usuario rechazó |
| `consent.revoked` | `consent-events` | Consent revocado |
| `consent.expired` | `consent-events` | TTL expirado |
| `consent.consumed` | `consent-events` | Datos accedidos |

## Stack

- Java 21 + Spring Boot 3.x
- Spring Data JPA (PostgreSQL)
- Spring Data Redis
- Spring Kafka
- Flyway (migraciones DB)

## Configuración

| Variable | Descripción | Default |
|---|---|---|
| `CONSENT_TTL_ACCOUNTS_DAYS` | TTL para consents de cuentas | 365 |
| `CONSENT_TTL_PAYMENTS_HOURS` | TTL para consents de pagos | 24 |
| `CONSENT_TTL_FUNDS_HOURS` | TTL para funds confirmation | 24 |
| `DB_URL` | PostgreSQL connection string | — |
| `REDIS_URL` | Redis connection string | — |
| `KAFKA_BROKERS` | Kafka bootstrap servers | — |

## Puertos

| Puerto | Protocolo | Uso |
|---|---|---|
| 8080 | HTTP | API interna (gRPC futuro) |
| 8081 | HTTP | Actuator (health, metrics) |
