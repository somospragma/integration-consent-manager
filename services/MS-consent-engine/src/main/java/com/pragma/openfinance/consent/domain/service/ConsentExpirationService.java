package com.pragma.openfinance.consent.domain.service;

import com.pragma.openfinance.consent.domain.model.Consent;
import com.pragma.openfinance.consent.domain.model.ConsentStatus;
import com.pragma.openfinance.consent.domain.port.ConsentRepository;
import com.pragma.openfinance.consent.infrastructure.event.ConsentEventPublisher;
import com.pragma.openfinance.consent.infrastructure.cache.ConsentCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentExpirationService {

    private final ConsentRepository consentRepository;
    private final ConsentEventPublisher eventPublisher;
    private final ConsentCacheService cacheService;

    /**
     * Job que se ejecuta cada minuto para expirar consentimientos vencidos.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireConsents() {
        List<Consent> expiredConsents = consentRepository
                .findByStatusAndExpiresAtBefore(ConsentStatus.AUTHORIZED, Instant.now());

        if (expiredConsents.isEmpty()) {
            return;
        }

        log.info("Expiring {} consents", expiredConsents.size());

        for (Consent consent : expiredConsents) {
            ConsentStatus previousStatus = consent.getStatus();
            consent.setStatus(ConsentStatus.EXPIRED);
            consentRepository.save(consent);
            cacheService.evictFromCache(consent.getConsentId());
            eventPublisher.publishConsentExpired(consent, previousStatus);
        }

        log.info("Expired {} consents successfully", expiredConsents.size());
    }
}
