package com.pragma.openfinance.consent.domain.service;

import com.pragma.openfinance.consent.domain.model.*;
import com.pragma.openfinance.consent.domain.port.ConsentRepository;
import com.pragma.openfinance.consent.infrastructure.event.ConsentEventPublisher;
import com.pragma.openfinance.consent.infrastructure.cache.ConsentCacheService;
import com.pragma.openfinance.consent.domain.exception.ConsentNotFoundException;
import com.pragma.openfinance.consent.domain.exception.InvalidConsentStateException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentService {

    private final ConsentRepository consentRepository;
    private final ConsentEventPublisher eventPublisher;
    private final ConsentCacheService cacheService;

    @Transactional
    public Consent createConsent(ConsentType type, String tppId, List<String> permissions, Instant expiresAt) {
        log.info("Creating consent for TPP: {}, type: {}", tppId, type);

        Consent consent = Consent.builder()
                .type(type)
                .status(ConsentStatus.AWAITING_AUTHORIZATION)
                .tppId(tppId)
                .permissions(permissions)
                .expiresAt(expiresAt)
                .build();

        consent = consentRepository.save(consent);

        eventPublisher.publishConsentCreated(consent);
        log.info("Consent created: {}", consent.getConsentId());

        return consent;
    }

    @Transactional(readOnly = true)
    public Consent getConsent(UUID consentId) {
        return cacheService.getFromCache(consentId)
                .orElseGet(() -> {
                    Consent consent = consentRepository.findById(consentId)
                            .orElseThrow(() -> new ConsentNotFoundException(consentId));
                    cacheService.putInCache(consent);
                    return consent;
                });
    }

    @Transactional
    public Consent authorizeConsent(UUID consentId, String userId, List<String> accountIds, String authMethod) {
        Consent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new ConsentNotFoundException(consentId));

        if (!consent.canBeAuthorized()) {
            throw new InvalidConsentStateException(consentId, consent.getStatus(), ConsentStatus.AUTHORIZED);
        }

        ConsentStatus previousStatus = consent.getStatus();
        consent.setStatus(ConsentStatus.AUTHORIZED);
        consent.setUserId(userId);
        consent.setAuthorizedAt(Instant.now());
        consent.setMetadata(ConsentMetadata.builder()
                .accountIds(accountIds)
                .authenticationMethod(authMethod)
                .build());

        consent = consentRepository.save(consent);
        cacheService.putInCache(consent);

        eventPublisher.publishConsentAuthorized(consent, previousStatus);
        log.info("Consent authorized: {}, user: {}", consentId, userId);

        return consent;
    }

    @Transactional
    public Consent rejectConsent(UUID consentId, String reason) {
        Consent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new ConsentNotFoundException(consentId));

        ConsentStatus previousStatus = consent.getStatus();
        consent.setStatus(ConsentStatus.REJECTED);

        consent = consentRepository.save(consent);
        cacheService.evictFromCache(consentId);

        eventPublisher.publishConsentRejected(consent, previousStatus, reason);
        log.info("Consent rejected: {}, reason: {}", consentId, reason);

        return consent;
    }

    @Transactional
    public Consent revokeConsent(UUID consentId, String actorId, ConsentEvent.ActorType actorType) {
        Consent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new ConsentNotFoundException(consentId));

        if (!consent.canBeRevoked()) {
            throw new InvalidConsentStateException(consentId, consent.getStatus(), ConsentStatus.REVOKED);
        }

        ConsentStatus previousStatus = consent.getStatus();
        consent.setStatus(ConsentStatus.REVOKED);
        consent.setRevokedAt(Instant.now());

        consent = consentRepository.save(consent);
        cacheService.evictFromCache(consentId);

        eventPublisher.publishConsentRevoked(consent, previousStatus, actorId, actorType);
        log.info("Consent revoked: {}, by: {} ({})", consentId, actorId, actorType);

        return consent;
    }

    @Transactional
    public Consent consumeConsent(UUID consentId) {
        Consent consent = consentRepository.findById(consentId)
                .orElseThrow(() -> new ConsentNotFoundException(consentId));

        if (consent.getStatus() != ConsentStatus.AUTHORIZED) {
            throw new InvalidConsentStateException(consentId, consent.getStatus(), ConsentStatus.CONSUMED);
        }

        ConsentStatus previousStatus = consent.getStatus();
        consent.setStatus(ConsentStatus.CONSUMED);

        consent = consentRepository.save(consent);
        cacheService.evictFromCache(consentId);

        eventPublisher.publishConsentConsumed(consent, previousStatus);
        log.info("Consent consumed: {}", consentId);

        return consent;
    }

    public boolean isConsentActive(UUID consentId) {
        Consent consent = getConsent(consentId);
        return consent.isActive();
    }

    public boolean hasPermission(UUID consentId, String requiredPermission) {
        Consent consent = getConsent(consentId);
        return consent.isActive() && consent.getPermissions().contains(requiredPermission);
    }
}
