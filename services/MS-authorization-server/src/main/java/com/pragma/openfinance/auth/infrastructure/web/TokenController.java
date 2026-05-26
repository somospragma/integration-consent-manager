package com.pragma.openfinance.auth.infrastructure.web;

import com.pragma.openfinance.auth.domain.service.ClientAuthenticationService;
import com.pragma.openfinance.auth.domain.service.TokenService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class TokenController {

    private final TokenService tokenService;
    private final ClientAuthenticationService clientAuthService;

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> token(
            @RequestParam("grant_type") String grantType,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "redirect_uri", required = false) String redirectUri,
            @RequestParam(value = "code_verifier", required = false) String codeVerifier,
            @RequestParam(value = "client_id", required = false) String clientId,
            @RequestParam(value = "scope", required = false) String scope,
            @RequestParam(value = "refresh_token", required = false) String refreshToken,
            @RequestParam(value = "client_assertion", required = false) String clientAssertion,
            @RequestParam(value = "client_assertion_type", required = false) String clientAssertionType,
            @RequestHeader(value = "X-Client-Cert-Thumbprint", required = false) String certThumbprint) {

        // Authenticate client
        if (clientAssertion != null) {
            var client = clientAuthService.authenticate(clientAssertion, clientAssertionType);
            clientId = client.getClientId();
        } else if (clientId != null) {
            clientAuthService.authenticateByClientId(clientId);
        } else {
            return ResponseEntity.status(401).body(Map.of("error", "invalid_client"));
        }

        Map<String, Object> response;

        switch (grantType) {
            case "authorization_code" -> {
                if (code == null || codeVerifier == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "invalid_request",
                            "error_description", "code and code_verifier are required"));
                }
                response = tokenService.issueTokensFromCode(code, codeVerifier, clientId, redirectUri, certThumbprint);
            }
            case "client_credentials" -> {
                if (scope == null) scope = "consents";
                response = tokenService.issueClientCredentialsToken(clientId, scope, certThumbprint);
            }
            case "refresh_token" -> {
                if (refreshToken == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "invalid_request",
                            "error_description", "refresh_token is required"));
                }
                response = tokenService.refreshToken(refreshToken, clientId, certThumbprint);
            }
            default -> {
                return ResponseEntity.badRequest().body(Map.of("error", "unsupported_grant_type"));
            }
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/revoke")
    public ResponseEntity<Void> revoke(
            @RequestParam("token") String token,
            @RequestParam(value = "token_type_hint", required = false) String tokenTypeHint) {

        tokenService.revokeToken(token);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/introspect")
    public ResponseEntity<Map<String, Object>> introspect(@RequestParam("token") String token) {
        return ResponseEntity.ok(tokenService.introspect(token));
    }
}
