package com.pragma.openfinance.consent.infrastructure.cache;

import com.pragma.openfinance.consent.domain.model.Consent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConsentCacheService {

    private static final String CACHE_PREFIX = "consent:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    private final RedisTemplate<String, Consent> redisTemplate;

    public Optional<Consent> getFromCache(UUID consentId) {
        String key = CACHE_PREFIX + consentId;
        Consent consent = redisTemplate.opsForValue().get(key);
        if (consent != null) {
            log.debug("Cache hit for consent: {}", consentId);
            return Optional.of(consent);
        }
        log.debug("Cache miss for consent: {}", consentId);
        return Optional.empty();
    }

    public void putInCache(Consent consent) {
        String key = CACHE_PREFIX + consent.getConsentId();
        redisTemplate.opsForValue().set(key, consent, CACHE_TTL);
        log.debug("Cached consent: {}", consent.getConsentId());
    }

    public void evictFromCache(UUID consentId) {
        String key = CACHE_PREFIX + consentId;
        redisTemplate.delete(key);
        log.debug("Evicted consent from cache: {}", consentId);
    }
}
