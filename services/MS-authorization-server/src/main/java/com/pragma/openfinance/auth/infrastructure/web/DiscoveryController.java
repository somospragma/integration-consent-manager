package com.pragma.openfinance.auth.infrastructure.web;

import com.pragma.openfinance.auth.domain.service.KeyManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class DiscoveryController {

    @Value("${auth.issuer:https://auth.openfinance.example.com}")
    private String issuer;

    private final KeyManagementService keyManagementService;

    @GetMapping("/.well-known/openid-configuration")
    public ResponseEntity<Map<String, Object>> discovery() {
        return ResponseEntity.ok(Map.ofEntries(
                Map.entry("issuer", issuer),
                Map.entry("authorization_endpoint", issuer + "/authorize"),
                Map.entry("token_endpoint", issuer + "/token"),
                Map.entry("pushed_authorization_request_endpoint", issuer + "/par"),
                Map.entry("revocation_endpoint", issuer + "/revoke"),
                Map.entry("introspection_endpoint", issuer + "/introspect"),
                Map.entry("userinfo_endpoint", issuer + "/userinfo"),
                Map.entry("registration_endpoint", issuer + "/register"),
                Map.entry("jwks_uri", issuer + "/.well-known/jwks.json"),
                Map.entry("scopes_supported", List.of("openid", "accounts", "payments", "consents", "funds-confirmations")),
                Map.entry("response_types_supported", List.of("code")),
                Map.entry("grant_types_supported", List.of("authorization_code", "client_credentials", "refresh_token")),
                Map.entry("token_endpoint_auth_methods_supported", List.of("private_key_jwt", "tls_client_auth")),
                Map.entry("token_endpoint_auth_signing_alg_values_supported", List.of("PS256", "ES256")),
                Map.entry("id_token_signing_alg_values_supported", List.of("PS256", "ES256")),
                Map.entry("code_challenge_methods_supported", List.of("S256")),
                Map.entry("tls_client_certificate_bound_access_tokens", true),
                Map.entry("require_pushed_authorization_requests", true),
                Map.entry("authorization_response_iss_parameter_supported", true),
                Map.entry("subject_types_supported", List.of("pairwise"))
        ));
    }

    @GetMapping("/.well-known/jwks.json")
    public ResponseEntity<Map<String, Object>> jwks() {
        return ResponseEntity.ok(keyManagementService.getJwks());
    }
}
