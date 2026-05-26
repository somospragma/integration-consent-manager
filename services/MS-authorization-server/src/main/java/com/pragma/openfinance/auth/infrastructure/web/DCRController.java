package com.pragma.openfinance.auth.infrastructure.web;

import com.pragma.openfinance.auth.domain.model.RegisteredClient;
import com.pragma.openfinance.auth.domain.service.DCRService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/register")
@RequiredArgsConstructor
public class DCRController {

    private final DCRService dcrService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> request) {
        RegisteredClient client = dcrService.registerClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(client));
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<Map<String, Object>> getClient(@PathVariable String clientId) {
        RegisteredClient client = dcrService.getClient(clientId);
        return ResponseEntity.ok(toResponse(client));
    }

    @PutMapping("/{clientId}")
    public ResponseEntity<Map<String, Object>> updateClient(
            @PathVariable String clientId,
            @RequestBody Map<String, Object> request) {
        RegisteredClient client = dcrService.updateClient(clientId, request);
        return ResponseEntity.ok(toResponse(client));
    }

    @DeleteMapping("/{clientId}")
    public ResponseEntity<Void> deleteClient(@PathVariable String clientId) {
        dcrService.deleteClient(clientId);
        return ResponseEntity.noContent().build();
    }

    private Map<String, Object> toResponse(RegisteredClient client) {
        return Map.of(
                "client_id", client.getClientId(),
                "client_id_issued_at", client.getClientIdIssuedAt().getEpochSecond(),
                "client_name", client.getClientName() != null ? client.getClientName() : "",
                "token_endpoint_auth_method", client.getClientAuthenticationMethods(),
                "grant_types", Arrays.asList(client.getAuthorizationGrantTypes().split(",")),
                "redirect_uris", client.getRedirectUris() != null ? Arrays.asList(client.getRedirectUris().split(",")) : java.util.List.of(),
                "scope", client.getScopes(),
                "software_id", client.getSoftwareId() != null ? client.getSoftwareId() : ""
        );
    }
}
