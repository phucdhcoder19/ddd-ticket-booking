package com.hoangphuc.ddd.infrastructure.cache.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * WAITING ROOM on Redis — three keys, nothing more:
 *
 *   queue:seq       increasing counter, hands out a "ticket number" to each newcomer
 *   queue:waiting   SORTED SET  people waiting,  score = ticket number
 *   queue:admitted  SORTED SET  people let in,   score = admission expiry (epoch ms)
 *
 * Why admitted is also a sorted set instead of one key per person with EX:
 * the admitter needs to know "how many people are inside". With a sorted set
 * that is one ZCARD. With separate keys it means scanning KEYS queue:admitted:*
 * — a command that blocks all of Redis, right when traffic is highest.
 *
 * Uses StringRedisTemplate, NOT the project's RedisTemplate<String, Object>:
 * that one serializes values as JSON, so token "Q-AAA" would sit in Redis as
 * "\"Q-AAA\"" — still correct, but confusing in redis-cli, and the Lua script
 * would receive ARGV wrapped in quotes. Here everything is a plain string.
 */
@Component
@RequiredArgsConstructor
public class WaitingRoomRedisStore {

    private static final String KEY_SEQ      = "queue:seq";
    private static final String KEY_WAITING  = "queue:waiting";
    private static final String KEY_ADMITTED = "queue:admitted";

    /**
     * ADMIT — must be ONE atomic operation, so it is written in Lua.
     *
     * Written in Java as three separate commands (count -> compute free slots ->
     * pop), two servers running the job at the same time would both see "20
     * slots free" and both admit 20 people: 40 inside, over capacity. Exactly
     * the read-then-write trap from the stock deduction lesson
     * (StockCacheService). Redis runs a script as a single command; nobody can
     * cut in.
     *
     *   KEYS[1] = queue:waiting     ARGV[1] = now (ms)
     *   KEYS[2] = queue:admitted    ARGV[2] = capacity
     *                               ARGV[3] = max people admitted per run
     *                               ARGV[4] = admission expiry (ms)
     *
     * Returns the number of people just admitted.
     */
    private static final String LUA_ADMIT =
            // ① Drop expired admissions: someone let in who walked away frees the slot after 15 minutes
            "redis.call('ZREMRANGEBYSCORE', KEYS[2], '-inf', ARGV[1]) " +
            // ② How many slots are free inside
            "local free = tonumber(ARGV[2]) - redis.call('ZCARD', KEYS[2]) " +
            "if free <= 0 then return 0 end " +
            "local batch = math.min(free, tonumber(ARGV[3])) " +
            // ③ Take the people at the front (lowest score = earliest) AND remove them from the queue
            "local popped = redis.call('ZPOPMIN', KEYS[1], batch) " +
            // ZPOPMIN returns [token1, score1, token2, score2, ...] so the step is 2
            "for i = 1, #popped, 2 do " +
            "  redis.call('ZADD', KEYS[2], ARGV[4], popped[i]) " +
            "end " +
            "return #popped / 2";

    private static final DefaultRedisScript<Long> SCRIPT_ADMIT =
            new DefaultRedisScript<>(LUA_ADMIT, Long.class);

    private final StringRedisTemplate redis;

    /**
     * Put a person at the back of the queue.
     *
     * The ticket number comes from INCR, not from the system clock: two people
     * arriving in the same millisecond would share a score, and two servers
     * whose clocks differ by a few dozen milliseconds would swap the order.
     * Redis INCR never returns a duplicate, even with ten servers calling it.
     */
    public void enqueue(String token) {
        Long seq = redis.opsForValue().increment(KEY_SEQ);
        redis.opsForZSet().add(KEY_WAITING, token, seq);
    }

    /** Number of people AHEAD of this token (0 = front of the queue). NULL if not in the queue. */
    public Long rankInQueue(String token) {
        return redis.opsForZSet().rank(KEY_WAITING, token);
    }

    public long waitingCount() {
        Long n = redis.opsForZSet().zCard(KEY_WAITING);
        return n == null ? 0 : n;
    }

    /** Admission expiry (epoch ms). NULL if this person was never admitted, or has been cleaned up. */
    public Long admittedUntil(String token) {
        Double score = redis.opsForZSet().score(KEY_ADMITTED, token);
        return score == null ? null : score.longValue();
    }

    /** Run the admit script. See LUA_ADMIT. */
    public long admit(long nowMs, int capacity, int maxBatch, long expireAtMs) {
        Long n = redis.execute(SCRIPT_ADMIT,
                List.of(KEY_WAITING, KEY_ADMITTED),
                String.valueOf(nowMs),
                String.valueOf(capacity),
                String.valueOf(maxBatch),
                String.valueOf(expireAtMs));
        return n == null ? 0 : n;
    }

    /** Give the slot inside to the next person — called when the customer finishes buying. */
    public void leave(String token) {
        redis.opsForZSet().remove(KEY_ADMITTED, token);
    }
}
