package com.pragma.openfinance.consent.domain.port;

import com.pragma.openfinance.consent.domain.model.Consent;
import com.pragma.openfinance.consent.domain.model.ConsentStatus;
import com.pragma.openfinance.consent.domain.model.ConsentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ConsentRepository extends JpaRepository<Consent, UUID> {

    Page<Consent> findByTppId(String tppId, Pageable pageable);

    Page<Consent> findByUserId(String userId, Pageable pageable);

    Page<Consent> findByTppIdAndStatus(String tppId, ConsentStatus status, Pageable pageable);

    Page<Consent> findByUserIdAndStatus(String userId, ConsentStatus status, Pageable pageable);

    List<Consent> findByStatusAndExpiresAtBefore(ConsentStatus status, Instant now);

    @Query("SELECT c FROM Consent c WHERE c.tppId = :tppId AND c.status = :status")
    List<Consent> findActiveByTpp(@Param("tppId") String tppId, @Param("status") ConsentStatus status);

    @Query("SELECT COUNT(c) FROM Consent c WHERE c.status = :status")
    long countByStatus(@Param("status") ConsentStatus status);

    @Query("SELECT COUNT(c) FROM Consent c WHERE c.tppId = :tppId AND c.status = :status")
    long countByTppAndStatus(@Param("tppId") String tppId, @Param("status") ConsentStatus status);
}
