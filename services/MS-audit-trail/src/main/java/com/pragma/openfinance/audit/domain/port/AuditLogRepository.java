package com.pragma.openfinance.audit.domain.port;

import com.pragma.openfinance.audit.domain.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByConsentIdOrderByCreatedAtDesc(UUID consentId, Pageable pageable);

    Page<AuditLog> findByActorIdOrderByCreatedAtDesc(String actorId, Pageable pageable);

    Page<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to, Pageable pageable);

    @Query("SELECT a FROM AuditLog a ORDER BY a.createdAt DESC LIMIT 1")
    Optional<AuditLog> findLatest();

    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.consentId = :consentId")
    long countByConsentId(@Param("consentId") UUID consentId);
}
