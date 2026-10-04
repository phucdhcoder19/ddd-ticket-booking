package com.hoangphuc.ddd.infrastructure.cache.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Raw storage for Idempotency-Key records: one Redis string per key.
 *
 * The meaning of the stored value belongs to the application layer
 * (IdempotencyAppService); this class only knows Redis commands.
 * StringRedisTemplate for the same reason as WaitingRoomRedisStore: plain
 * strings, readable in redis-cli, no JSON wrapping around the value.
 */
@Component
@RequiredArgsConstructor
public class IdempotencyRedisStore {

    private final StringRedisTemplate redis;

    /**
     * SET key value NX EX ttl — succeeds for exactly ONE caller.
     *
     * NX is the whole trick: two identical requests arriving in the same
     * millisecond on two different servers both try this, Redis runs them one
     * after the other, and only the first finds the key absent.
     */
    public boolean claim(String key, String value, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, value, ttl));
    }

    public String get(String key) {
        return redis.opsForValue().get(key);
    }

    /** Overwrite the "processing" marker with the final result. */
    public void complete(String key, String value, Duration ttl) {
        redis.opsForValue().set(key, value, ttl);
    }

    public void release(String key) {
        redis.delete(key);
    }
}
