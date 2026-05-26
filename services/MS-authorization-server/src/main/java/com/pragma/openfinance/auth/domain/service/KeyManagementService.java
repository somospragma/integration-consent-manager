package com.pragma.openfinance.auth.domain.service;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Gestión de claves de firma del Authorization Server.
 * En producción, las claves se almacenan en Vault/KMS.
 * Soporta rotación con período de gracia (clave anterior activa 24h).
 */
@Service
@Slf4j
public class KeyManagementService {

    private RSAKey currentKey;
    private RSAKey previousKey;
    private String currentKeyId;

    @PostConstruct
    public void init() {
        rotateKeys();
        log.info("Key management initialized with key: {}", currentKeyId);
    }

    public RSAPrivateKey getCurrentSigningKey() {
        try {
            return currentKey.toRSAPrivateKey();
        } catch (Exception e) {
            throw new RuntimeException("Failed to get signing key", e);
        }
    }

    public String getCurrentKeyId() {
        return currentKeyId;
    }

    public Map<String, Object> getJwks() {
        List<RSAKey> keys = new ArrayList<>();
        keys.add(currentKey.toPublicJWK());
        if (previousKey != null) {
            keys.add(previousKey.toPublicJWK());
        }
        JWKSet jwkSet = new JWKSet(new ArrayList<>(keys));
        return jwkSet.toJSONObject();
    }

    /**
     * Rotación de claves. En producción se ejecuta cada 90 días.
     * Para la PoC se puede invocar manualmente.
     */
    @Scheduled(fixedRate = 7776000000L) // 90 días en ms
    public void rotateKeys() {
        previousKey = currentKey;
        currentKeyId = "pragma-auth-" + UUID.randomUUID().toString().substring(0, 8);

        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();

            currentKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(currentKeyId)
                    .build();

            log.info("Key rotated. New key ID: {}", currentKeyId);
        } catch (Exception e) {
            throw new RuntimeException("Key generation failed", e);
        }
    }
}
