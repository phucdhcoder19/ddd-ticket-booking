package com.hoangphuc.ddd.infrastructure.cache.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Failed sign-in counter, one key per phone: login:fail:0912345678.
 *
 * In Redis, not in memory: an attacker spreading guesses over 3 servers would
 * get 3x the attempts if each server counted on its own.
 */
@Component
@RequiredArgsConstructor
public class LoginAttemptRedisStore {

    /**
     * INCR and set the expiry in ONE atomic step. As two commands, a crash
     * between them leaves a counter with no TTL — and since a locked-out phone
     * is rejected before counting again, nothing would ever set it: locked forever.
     */
    private static final DefaultRedisScript<Long> SCRIPT_FAIL = new DefaultRedisScript<>(
            "local n = redis.call('INCR', KEYS[1]) " +
            "if n == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end " +
            "return n", Long.class);

    private final StringRedisTemplate redis;

    /** @return failures so far in the current window, this one included */
    public long recordFailure(String phone, Duration window) {
        Long n = redis.execute(SCRIPT_FAIL, List.of(key(phone)), String.valueOf(window.toMillis()));
        return n == null ? 0 : n;
    }

    public long failures(String phone) {
        String v = redis.opsForValue().get(key(phone));
        return v == null ? 0 : Long.parseLong(v);
    }

    public void reset(String phone) {
        redis.delete(key(phone));
    }

    private String key(String phone) {
        return "login:fail:" + phone;
    }
}
