package com.pragma.openfinance.auth.domain.service;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

/**
 * Servicio de generación de JWT firmados.
 * Implementa certificate binding (cnf claim) para FAPI 2.0.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    @Value("${auth.issuer:https://auth.openfinance.example.com}")
    private String issuer;

    @Value("${auth.signing-alg:PS256}")
    private String signingAlgorithm;

    private final KeyManagementService keyManagementService;

    public String generateAccessToken(String subject, String clientId, String scope,
                                       String consentId, String certThumbprint, int ttlSeconds) {
        try {
            RSAPrivateKey privateKey = keyManagementService.getCurrentSigningKey();
            String keyId = keyManagementService.getCurrentKeyId();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.parse(signingAlgorithm))
                    .type(new JOSEObjectType("at+jwt"))
                    .keyID(keyId)
                    .build();

            JWTClaimsSet.Builder claimsBuilder = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(subject)
                    .audience("https://api.openfinance.example.com")
                    .claim("client_id", clientId)
                    .claim("scope", scope)
                    .jwtID(UUID.randomUUID().toString())
                    .issueTime(Date.from(Instant.now()))
                    .notBeforeTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusSeconds(ttlSeconds)));

            // Consent binding
            if (consentId != null) {
                claimsBuilder.claim("consent_id", consentId);
            }

            // Certificate binding (FAPI 2.0 sender-constrained tokens)
            if (certThumbprint != null) {
                claimsBuilder.claim("cnf", Map.of("x5t#S256", certThumbprint));
            }

            SignedJWT signedJWT = new SignedJWT(header, claimsBuilder.build());
            signedJWT.sign(new RSASSASigner(privateKey));

            return signedJWT.serialize();

        } catch (Exception e) {
            log.error("Failed to generate access token", e);
            throw new RuntimeException("Token generation failed", e);
        }
    }

    public String generateIdToken(String subject, String clientId) {
        try {
            RSAPrivateKey privateKey = keyManagementService.getCurrentSigningKey();
            String keyId = keyManagementService.getCurrentKeyId();

            JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.parse(signingAlgorithm))
                    .keyID(keyId)
                    .build();

            JWTClaimsSet claims = new JWTClaimsSet.Builder()
                    .issuer(issuer)
                    .subject(subject)
                    .audience(clientId)
                    .issueTime(Date.from(Instant.now()))
                    .expirationTime(Date.from(Instant.now().plusSeconds(900)))
                    .jwtID(UUID.randomUUID().toString())
                    .build();

            SignedJWT signedJWT = new SignedJWT(header, claims);
            signedJWT.sign(new RSASSASigner(privateKey));

            return signedJWT.serialize();

        } catch (Exception e) {
            log.error("Failed to generate ID token", e);
            throw new RuntimeException("ID token generation failed", e);
        }
    }
}
