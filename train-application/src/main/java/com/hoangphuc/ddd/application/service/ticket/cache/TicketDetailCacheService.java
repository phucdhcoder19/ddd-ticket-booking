package com.hoangphuc.ddd.application.service.ticket.cache;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.service.TicketDetailDomainService;
import com.hoangphuc.ddd.infrastructure.cache.redis.RedisInfrasService;
import com.hoangphuc.ddd.infrastructure.distributed.DistributedLockService;
import com.hoangphuc.ddd.infrastructure.distributed.DistributedLocker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * SECOND LINE OF DEFENCE — stops requests before they reach MySQL.
 *
 * Cache-Aside + a Redisson lock against CACHE STAMPEDE:
 *   1. Ask the cache
 *   2. Empty -> take the lock; only 1 thread may go down to MySQL
 *   3. Once the lock is held, ASK THE CACHE AGAIN (double-check)
 *   4. Load from MySQL, write the cache, release the lock
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketDetailCacheService {

    /** The cache lives 10 minutes and then expires. NEVER cache forever. */
    private static final Duration TTL = Duration.ofMinutes(10);

    /** Wait at most 1s for the lock. Give up after that, never queue forever. */
    private static final long LOCK_WAIT_SECONDS = 1;

    /** Hold the lock at most 5s. If the server dies midway, Redis deletes the lock. */
    private static final long LOCK_LEASE_SECONDS = 5;

    private final RedisInfrasService redisInfrasService;
    private final TicketDetailDomainService ticketDetailDomainService;
    private final DistributedLockService distributedLockService;

    public TicketDetail getTicketDetail(Long ticketId) {
        String key = genKey(ticketId);

        // ---- STEP 1: ask Redis ----
        TicketDetail cached = redisInfrasService.getObject(key, TicketDetail.class);
        if (cached != null) {
            log.info("[CACHE] HIT  | key={} -> MySQL NOT touched", key);
            return cached;
        }

        log.info("[CACHE] MISS | key={} -> need the lock to go to MySQL", key);

        // ---- STEP 2: take the lock ----
        // Without this step: 5000 threads all see an empty cache in STEP 1
        // and all rush to MySQL -> cache stampede -> the DB chokes.
        DistributedLocker locker = distributedLockService.getLock(genLockKey(ticketId));
        boolean locked = false;
        try {
            locked = locker.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);

            if (!locked) {
                // Someone else is loading. Still not done after 1s -> read the cache one last time.
                log.warn("[LOCK] could not acquire the lock | key={} -> reading cache again", key);
                return redisInfrasService.getObject(key, TicketDetail.class);
            }

            // ---- STEP 3: DOUBLE-CHECK — the most important step ----
            // The previous thread may have finished loading while we waited.
            // Without this step the 5000 threads still go to MySQL one by one,
            // the only difference being that they queue instead of rushing at once.
            cached = redisInfrasService.getObject(key, TicketDetail.class);
            if (cached != null) {
                log.info("[CACHE] HIT after waiting for the lock | key={} -> MySQL NOT touched", key);
                return cached;
            }

            // ---- STEP 4: only ONE thread gets here ----
            log.info("[CACHE] loading from MySQL | key={}", key);
            TicketDetail fromDb = ticketDetailDomainService.getTicketDetailById(ticketId);
            if (fromDb == null) {
                return null;
            }

            redisInfrasService.setObject(key, fromDb, TTL);
            log.info("[CACHE] FILL | key={} ttl={}min", key, TTL.toMinutes());
            return fromDb;

        } catch (InterruptedException e) {
            // The thread was interrupted while waiting for the lock -> restore the flag and exit
            Thread.currentThread().interrupt();
            log.warn("[LOCK] interrupted while waiting for the lock | key={}", key);
            return null;

        } finally {
            // The lock MUST be released in finally — otherwise it stays stuck until
            // the lease runs out, and every request for this ticket stalls for 5 seconds.
            if (locked) {
                locker.unlock();
            }
        }
    }

    /**
     * Drop the cache when the source data changes (purchase, ticket edit...).
     * Delete rather than update — simpler and less error-prone.
     */
    public void evict(Long ticketId) {
        String key = genKey(ticketId);
        redisInfrasService.delete(key);
        log.info("[CACHE] EVICT| key={}", key);
    }

    private String genKey(Long ticketId) {
        return "TICKET:DETAIL:" + ticketId;
    }

    /** The lock must use a SEPARATE key, not the cache key. */
    private String genLockKey(Long ticketId) {
        return "LOCK:TICKET:DETAIL:" + ticketId;
    }
}
