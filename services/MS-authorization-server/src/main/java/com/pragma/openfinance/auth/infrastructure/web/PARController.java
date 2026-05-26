package com.pragma.openfinance.auth.infrastructure.web;

import com.pragma.openfinance.auth.domain.model.PushedAuthorizationRequest;
import com.pragma.openfinance.auth.domain.service.ClientAuthenticationService;
import com.pragma.openfinance.auth.domain.service.PARService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class PARController {

    private final PARService parService;
    private final ClientAuthenticationService clientAuthService;

    @PostMapping("/par")
    public ResponseEntity<Map<String, Object>> pushedAuthorizationRequest(
            @RequestParam("response_type") String responseType,
            @RequestParam("client_id") String clientId,
            @RequestParam("redirect_uri") String redirectUri,
            @RequestParam("scope") String scope,
            @RequestParam("code_challenge") String codeChallenge,
            @RequestParam("code_challenge_method") String codeChallengeMethod,
            @RequestParam(value = "client_assertion", required = false) String clientAssertion,
            @RequestParam(value = "client_assertion_type", required = false) String clientAssertionType,
            @RequestParam(value = "claims", required = false) String claims,
            @RequestParam(value = "state", required = false) String state) {

        // Authenticate client
        if (clientAssertion != null) {
            clientAuthService.authenticate(clientAssertion, clientAssertionType);
        } else {
            clientAuthService.authenticateByClientId(clientId);
        }

        // Validate response_type
        if (!"code".equals(responseType)) {
            return ResponseEntity.badRequest().body(Map.of("error", "unsupported_response_type"));
        }

        // Validate code_challenge_method
        if (!"S256".equals(codeChallengeMethod)) {
            return ResponseEntity.badRequest().body(Map.of("error", "invalid_request",
                    "error_description", "Only S256 code_challenge_method is supported"));
        }

        // Extract consent_id from claims
        String consentId = extractConsentId(claims);

        // Create PAR
        PushedAuthorizationRequest par = parService.create(
                clientId, responseType, redirectUri, scope,
                codeChallenge, codeChallengeMethod, consentId, state);

        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "request_uri", par.getRequestUri(),
                "expires_in", 60
        ));
    }

    private String extractConsentId(String claims) {
        if (claims == null) return null;
        // Extract openbanking_intent_id from claims JSON
        try {
            int idx = claims.indexOf("openbanking_intent_id");
            if (idx > 0) {
                int valueIdx = claims.indexOf("\"value\"", idx);
                int start = claims.indexOf("\"", valueIdx + 7) + 1;
                int end = claims.indexOf("\"", start);
                return claims.substring(start, end);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
