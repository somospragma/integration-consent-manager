package com.pragma.openfinance.auth.domain.service;

import com.pragma.openfinance.auth.domain.model.RegisteredClient;
import com.pragma.openfinance.auth.domain.port.RegisteredClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Dynamic Client Registration (DCR) - RFC 7591.
 * Registra TPPs automáticamente usando Software Statement Assertion del directorio.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DCRService {

    private final RegisteredClientRepository clientRepository;

    @Transactional
    public RegisteredClient registerClient(Map<String, Object> request) {
        String softwareStatement = (String) request.get("software_statement");
        String redirectUris = String.join(",", (Iterable<String>) request.getOrDefault("redirect_uris", java.util.List.of()));
        String tokenAuthMethod = (String) request.getOrDefault("token_endpoint_auth_method", "private_key_jwt");
        String grantTypes = String.join(",", (Iterable<String>) request.getOrDefault("grant_types",
                java.util.List.of("authorization_code", "client_credentials", "refresh_token")));
        String scope = (String) request.getOrDefault("scope", "openid accounts");

        // TODO: En producción - validar firma del SSA con JWKS del directorio (Raidiam)
        String softwareId = extractSoftwareIdFromSSA(softwareStatement);

        // Verificar si ya existe
        if (softwareId != null && clientRepository.findBySoftwareId(softwareId).isPresent()) {
            throw new IllegalArgumentException("Software already registered: " + softwareId);
        }

        String clientId = "pragma-" + UUID.randomUUID().toString().substring(0, 12);
        String registrationToken = UUID.randomUUID().toString();

        RegisteredClient client = RegisteredClient.builder()
                .id(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientName((String) request.getOrDefault("client_name", "TPP-" + clientId))
                .clientAuthenticationMethods(tokenAuthMethod)
                .authorizationGrantTypes(grantTypes)
                .redirectUris(redirectUris)
                .scopes(scope)
                .softwareId(softwareId)
                .softwareStatement(softwareStatement)
                .tokenSettings("{\"registration_access_token\":\"" + registrationToken + "\"}")
                .status("ACTIVE")
                .build();

        client = clientRepository.save(client);
        log.info("Client registered via DCR: {} (software: {})", clientId, softwareId);

        return client;
    }

    @Transactional(readOnly = true)
    public RegisteredClient getClient(String clientId) {
        return clientRepository.findByClientId(clientId)
                .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));
    }

    @Transactional
    public RegisteredClient updateClient(String clientId, Map<String, Object> request) {
        RegisteredClient client = getClient(clientId);

        if (request.containsKey("redirect_uris")) {
            client.setRedirectUris(String.join(",", (Iterable<String>) request.get("redirect_uris")));
        }
        if (request.containsKey("scope")) {
            client.setScopes((String) request.get("scope"));
        }
        if (request.containsKey("token_endpoint_auth_method")) {
            client.setClientAuthenticationMethods((String) request.get("token_endpoint_auth_method"));
        }

        client = clientRepository.save(client);
        log.info("Client updated via DCR: {}", clientId);
        return client;
    }

    @Transactional
    public void deleteClient(String clientId) {
        RegisteredClient client = getClient(clientId);
        client.setStatus("DELETED");
        clientRepository.save(client);
        log.info("Client deleted via DCR: {}", clientId);
    }

    private String extractSoftwareIdFromSSA(String ssa) {
        // Simplificado - en producción se parsea el JWT del SSA
        if (ssa != null && ssa.contains(".")) {
            try {
                String[] parts = ssa.split("\\.");
                String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                int idx = payload.indexOf("\"software_id\"");
                if (idx > 0) {
                    int start = payload.indexOf("\"", idx + 13) + 1;
                    int end = payload.indexOf("\"", start);
                    return payload.substring(start, end);
                }
            } catch (Exception ignored) {}
        }
        return "sw-" + UUID.randomUUID().toString().substring(0, 8);
    }
}
