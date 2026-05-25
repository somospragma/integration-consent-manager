package com.pragma.openfinance.orchestrator.payment.infrastructure.store;

import com.pragma.openfinance.orchestrator.payment.domain.model.PaymentConsentContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentFlowContextStore {

    private static final String KEY_PREFIX = "payment-consent-flow:";
    private static final Duration ACTIVE_TTL = Duration.ofMinutes(15);
    private static final Duration COMPLETED_TTL = Duration.ofHours(24);

    private final RedisTemplate<String, PaymentConsentContext> redisTemplate;

    public void save(PaymentConsentContext context) {
        String key = KEY_PREFIX + context.getFlowId();
        Duration ttl = context.isTerminal() ? COMPLETED_TTL : ACTIVE_TTL;
        redisTemplate.opsForValue().set(key, context, ttl);
    }

    public PaymentConsentContext get(UUID flowId) {
        String key = KEY_PREFIX + flowId;
        PaymentConsentContext context = redisTemplate.opsForValue().get(key);
        if (context == null) {
            throw new RuntimeException("Payment consent flow not found: " + flowId);
        }
        return context;
    }
}
