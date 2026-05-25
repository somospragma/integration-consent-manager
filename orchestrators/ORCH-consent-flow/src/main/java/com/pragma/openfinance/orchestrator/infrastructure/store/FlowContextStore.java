package com.pragma.openfinance.orchestrator.infrastructure.store;

import com.pragma.openfinance.orchestrator.domain.model.ConsentFlowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.UUID;

/**
 * Almacena el contexto del flujo en Redis.
 * TTL de 10 minutos para flujos activos (SCA timeout = 5 min).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FlowContextStore {

    private static final String KEY_PREFIX = "consent-flow:";
    private static final Duration ACTIVE_TTL = Duration.ofMinutes(10);
    private static final Duration COMPLETED_TTL = Duration.ofHours(1);

    private final RedisTemplate<String, ConsentFlowContext> redisTemplate;

    public void save(ConsentFlowContext context) {
        String key = KEY_PREFIX + context.getFlowId();
        Duration ttl = context.isTerminal() ? COMPLETED_TTL : ACTIVE_TTL;
        redisTemplate.opsForValue().set(key, context, ttl);
        log.debug("Saved flow context: {}, state: {}", context.getFlowId(), context.getState());
    }

    public ConsentFlowContext get(UUID flowId) {
        String key = KEY_PREFIX + flowId;
        ConsentFlowContext context = redisTemplate.opsForValue().get(key);
        if (context == null) {
            throw new FlowNotFoundException(flowId);
        }
        return context;
    }

    public boolean exists(UUID flowId) {
        String key = KEY_PREFIX + flowId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void delete(UUID flowId) {
        String key = KEY_PREFIX + flowId;
        redisTemplate.delete(key);
    }

    public static class FlowNotFoundException extends RuntimeException {
        public FlowNotFoundException(UUID flowId) {
            super("Consent flow not found: " + flowId);
        }
    }
}
