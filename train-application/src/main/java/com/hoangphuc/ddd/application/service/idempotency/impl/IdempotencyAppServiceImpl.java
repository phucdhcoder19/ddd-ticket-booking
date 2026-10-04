package com.hoangphuc.ddd.application.service.idempotency.impl;

import com.hoangphuc.ddd.application.service.idempotency.IdempotencyAppService;
import com.hoangphuc.ddd.infrastructure.cache.redis.IdempotencyRedisStore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Idempotency records on Redis, one key per (scope, user, Idempotency-Key):
 *
 *   idem:holds:1:7f3c...   "P:<fingerprint>"              first request still running
 *   idem:holds:1:7f3c...   "D:<fingerprint>:<response>"   done, response stored for replay
 *
 * WHAT THIS PROTECTS AGAINST. The frontend keeps the same key across retries
 * of one action. Without this service:
 *   - "Hold" clicked twice, or retried after a timeout whose first request
 *     actually succeeded: the second request finds the seats held (by the
 *     user's own first request) and answers "seat taken" for seats they own.
 *   - "Pay" retried after a timeout: the order exists, but the retry finds the
 *     hold already used and answers "your hold has expired" — to a customer
 *     who has just paid.
 * With it, the retry gets the first request's answer word for word.
 *
 * WHY ONLY SUCCESSES ARE STORED. A failed hold or order rolled back and
 * changed nothing, so running it again is harmless — and the user may have
 * changed their seat selection in between. Replaying a stored failure would
 * keep showing an old error for a request that could now succeed.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class IdempotencyAppServiceImpl implements IdempotencyAppService {

    private static final String PROCESSING = "P:";
    private static final String DONE = "D:";
    private static final long POLL_MS = 100;

    private final IdempotencyRedisStore store;

    /**
     * How long the "processing" marker lives. It only matters if the server
     * dies mid-request: the marker then frees itself, and a retry can go
     * through, instead of being stuck behind a request that will never finish.
     */
    @Value("${app.idempotency.processing-ttl:30s}")
    private Duration processingTtl;

    /** How long a successful response can be replayed. */
    @Value("${app.idempotency.result-ttl:1h}")
    private Duration resultTtl;

    /**
     * How long a duplicate waits for the first request to finish. A
     * double-click arrives a few hundred ms after the first click, while that
     * request is still running; waiting briefly lets it receive the real
     * result instead of an error. This holds a Tomcat thread, but only for
     * duplicates, which are rare.
     */
    @Value("${app.idempotency.wait-for-first:3s}")
    private Duration waitForFirst;

    @Override
    public Claim begin(String scope, Long userId, String key, String fingerprint) {
        String redisKey = redisKey(scope, userId, key);
        long deadline = System.currentTimeMillis() + waitForFirst.toMillis();

        while (true) {
            if (store.claim(redisKey, PROCESSING + fingerprint, processingTtl)) {
                return new Claim(Outcome.FRESH, null);
            }

            String value = store.get(redisKey);
            if (value != null) {
                boolean done = value.startsWith(DONE);
                String rest = value.substring(2);
                int sep = rest.indexOf(':');
                String storedFingerprint = done ? rest.substring(0, sep) : rest;

                if (!storedFingerprint.equals(fingerprint)) {
                    log.warn("[IDEM] key reused for a different request | {}", redisKey);
                    return new Claim(Outcome.KEY_REUSED, null);
                }
                if (done) {
                    log.info("[IDEM] replaying stored response | {}", redisKey);
                    return new Claim(Outcome.REPLAY, rest.substring(sep + 1));
                }
            }
            // value == null: the first request failed and released the key
            // between our claim and our get -> loop and try to claim it ourselves

            if (System.currentTimeMillis() >= deadline) {
                return new Claim(Outcome.IN_PROGRESS, null);
            }
            try {
                Thread.sleep(POLL_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return new Claim(Outcome.IN_PROGRESS, null);
            }
        }
    }

    @Override
    public void complete(String scope, Long userId, String key, String fingerprint, String response) {
        store.complete(redisKey(scope, userId, key), DONE + fingerprint + ":" + response, resultTtl);
    }

    @Override
    public void abandon(String scope, Long userId, String key) {
        store.release(redisKey(scope, userId, key));
    }

    private String redisKey(String scope, Long userId, String key) {
        return "idem:" + scope + ":" + userId + ":" + key;
    }
}
