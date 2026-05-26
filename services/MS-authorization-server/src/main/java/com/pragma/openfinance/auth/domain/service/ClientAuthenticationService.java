package com.pragma.openfinance.auth.domain.service;

import com.pragma.openfinance.auth.domain.model.RegisteredClient;
import com.pragma.openfinance.auth.domain.port.RegisteredClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Servicio de autenticación de clientes (TPPs).
 * Valida private_key_jwt y previene replay attacks (JTI tracking).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ClientAuthenticationService {

    private static final String JTI_PREFIX = "jti:";
    private static final Duration JTI_TTL = Duration.ofMinutes(5);

    private final RegisteredClientRepository clientRepository;
    private final RedisTemplate<String, String> jtiRedisTemplate;

    /**
     * Valida la autenticación del cliente via private_key_jwt.
     * En producción: parsear el JWT, validar firma con JWKS del directorio,
     * verificar claims (iss, sub, aud, exp, jti).
     */
    public RegisteredClient authenticate(String clientAssertion, String clientAssertionType) {
        if (!"urn:ietf:params:oauth:client-assertion-type:jwt-bearer".equals(clientAssertionType)) {
            throw new IllegalArgumentException("Unsupported client assertion type");
        }

        // TODO: En producción - parsear JWT, validar firma con JWKS del directorio
        // Por ahora extraemos client_id del assertion (simplificado para PoC)
        String clientId = extractClientIdFromAssertion(clientAssertion);
        String jti = extractJtiFromAssertion(clientAssertion);

        // Replay prevention
        if (jti != null) {
            String jtiKey = JTI_PREFIX + jti;
            Boolean wasAbsent = jtiRedisTemplate.opsForValue().setIfAbsent(jtiKey, "used", JTI_TTL);
            if (Boolean.FALSE.equals(wasAbsent)) {
                throw new IllegalArgumentException("JTI already used (replay attack detected)");
            }
        }

        RegisteredClient client = clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));

        if (!"ACTIVE".equals(client.getStatus())) {
            throw new IllegalArgumentException("Client is not active: " + clientId);
        }

        log.debug("Client authenticated: {}", clientId);
        return client;
    }

    /**
     * Autenticación simplificada por client_id (para endpoints que no requieren assertion).
     */
    public RegisteredClient authenticateByClientId(String clientId) {
        return clientRepository.findByClientId(clientId)
                .filter(c -> "ACTIVE".equals(c.getStatus()))
                .orElseThrow(() -> new IllegalArgumentException("Client not found or inactive: " + clientId));
    }

    // Simplificado - en producción se parsea el JWT completo
    private String extractClientIdFromAssertion(String assertion) {
        // En producción: decodificar JWT y extraer claim "sub"
        // Para PoC: el assertion ES el client_id directamente
        if (assertion != null && assertion.contains(".")) {
            // Es un JWT real - extraer sub del payload
            try {
                String[] parts = assertion.split("\\.");
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                // Extraer "sub" del JSON (simplificado)
                int subIdx = payload.indexOf("\"sub\"");
                if (subIdx > 0) {
                    int start = payload.indexOf("\"", subIdx + 5) + 1;
                    int end = payload.indexOf("\"", start);
                    return payload.substring(start, end);
                }
            } catch (Exception e) {
                log.warn("Failed to parse JWT assertion, using as client_id");
            }
        }
        return assertion;
    }

    private String extractJtiFromAssertion(String assertion) {
        if (assertion != null && assertion.contains(".")) {
            try {
                String[] parts = assertion.split("\\.");
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                int jtiIdx = payload.indexOf("\"jti\"");
                if (jtiIdx > 0) {
                    int start = payload.indexOf("\"", jtiIdx + 5) + 1;
                    int end = payload.indexOf("\"", start);
                    return payload.substring(start, end);
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
