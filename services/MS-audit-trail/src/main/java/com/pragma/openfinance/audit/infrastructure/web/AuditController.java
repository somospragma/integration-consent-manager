package com.pragma.openfinance.audit.infrastructure.web;

import com.pragma.openfinance.audit.domain.model.AuditLog;
import com.pragma.openfinance.audit.domain.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1/audit")
@RequiredArgsConstructor
public class AuditController {

    private final AuditService auditService;

    @GetMapping
    public ResponseEntity<Page<AuditLog>> queryAuditLogs(
            @RequestParam(required = false) UUID consentId,
            @RequestParam(required = false) String actorId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {

        PageRequest pageable = PageRequest.of(page, Math.min(size, 100));

        if (consentId != null) {
            return ResponseEntity.ok(auditService.getByConsentId(consentId, pageable));
        }
        if (actorId != null) {
            return ResponseEntity.ok(auditService.getByActorId(actorId, pageable));
        }
        if (from != null && to != null) {
            return ResponseEntity.ok(auditService.getByDateRange(from, to, pageable));
        }

        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/integrity-check")
    public ResponseEntity<Map<String, Object>> checkIntegrity(
            @RequestParam(defaultValue = "1000") int sampleSize) {

        boolean valid = auditService.verifyIntegrity(sampleSize);

        return ResponseEntity.ok(Map.of(
                "valid", valid,
                "sampleSize", sampleSize,
                "checkedAt", Instant.now().toString()
        ));
    }
}
