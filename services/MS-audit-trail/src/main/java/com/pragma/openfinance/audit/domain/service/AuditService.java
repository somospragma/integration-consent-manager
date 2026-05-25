package com.pragma.openfinance.audit.domain.service;

import com.pragma.openfinance.audit.domain.model.AuditLog;
import com.pragma.openfinance.audit.domain.port.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditService {

    private static final String GENESIS_HASH = "0000000000000000000000000000000000000000000000000000000000000000";

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public AuditLog createAuditEntry(UUID consentId, String action, String actorId,
                                      AuditLog.ActorType actorType, String previousState,
                                      String newState, String ipAddress, String requestSummary) {

        String previousHash = auditLogRepository.findLatest()
                .map(AuditLog::getHash)
                .orElse(GENESIS_HASH);

        String currentHash = computeHash(consentId, action, actorId, previousState, newState, previousHash);

        AuditLog auditLog = AuditLog.builder()
                .consentId(consentId)
                .action(action)
                .actorId(actorId)
                .actorType(actorType)
                .previousState(previousState)
                .newState(newState)
                .ipAddress(maskIpAddress(ipAddress))
                .requestSummary(requestSummary)
                .hash(currentHash)
                .previousHash(previousHash)
                .build();

        auditLog = auditLogRepository.save(auditLog);
        log.info("Audit entry created: {} for consent: {}", auditLog.getAuditId(), consentId);

        return auditLog;
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByConsentId(UUID consentId, Pageable pageable) {
        return auditLogRepository.findByConsentIdOrderByCreatedAtDesc(consentId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByActorId(String actorId, Pageable pageable) {
        return auditLogRepository.findByActorIdOrderByCreatedAtDesc(actorId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<AuditLog> getByDateRange(Instant from, Instant to, Pageable pageable) {
        return auditLogRepository.findByCreatedAtBetweenOrderByCreatedAtDesc(from, to, pageable);
    }

    /**
     * Verifica la integridad de la cadena de hashes.
     * Retorna true si todos los hashes son consistentes.
     */
    @Transactional(readOnly = true)
    public boolean verifyIntegrity(int sampleSize) {
        Page<AuditLog> logs = auditLogRepository.findAll(
                Pageable.ofSize(sampleSize));

        AuditLog previous = null;
        for (AuditLog current : logs) {
            if (previous != null && !current.getPreviousHash().equals(previous.getHash())) {
                log.error("Integrity violation detected at audit: {}", current.getAuditId());
                return false;
            }
            previous = current;
        }

        log.info("Integrity check passed for {} records", logs.getNumberOfElements());
        return true;
    }

    private String computeHash(UUID consentId, String action, String actorId,
                                String previousState, String newState, String previousHash) {
        String data = String.join("|",
                consentId.toString(),
                action,
                actorId,
                previousState != null ? previousState : "",
                newState,
                previousHash,
                Instant.now().toString()
        );

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }

    private String maskIpAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            return null;
        }
        // Mask last octet: 192.168.1.100 → 192.168.1.xxx
        int lastDot = ipAddress.lastIndexOf('.');
        if (lastDot > 0) {
            return ipAddress.substring(0, lastDot) + ".xxx";
        }
        return ipAddress;
    }
}
