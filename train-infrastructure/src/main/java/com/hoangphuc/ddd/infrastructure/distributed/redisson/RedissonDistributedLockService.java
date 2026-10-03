package com.hoangphuc.ddd.infrastructure.distributed.redisson;

import com.hoangphuc.ddd.infrastructure.distributed.DistributedLockService;
import com.hoangphuc.ddd.infrastructure.distributed.DistributedLocker;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Redisson implementation. This is the ONLY place in the project that
 * imports org.redisson.* — everything else only knows DistributedLocker.
 */
@Service
@RequiredArgsConstructor
public class RedissonDistributedLockService implements DistributedLockService {

    private final RedissonClient redissonClient;

    @Override
    public DistributedLocker getLock(String lockKey) {
        RLock rLock = redissonClient.getLock(lockKey);

        return new DistributedLocker() {

            @Override
            public boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException {
                return rLock.tryLock(waitTime, leaseTime, unit);
            }

            @Override
            public void unlock() {
                // ONLY release when the current thread holds it.
                // Without this check, thread A could release thread B's lock
                // (when A's lease has expired and B has just acquired it).
                if (rLock.isHeldByCurrentThread()) {
                    rLock.unlock();
                }
            }

            @Override
            public boolean isHeldByCurrentThread() {
                return rLock.isHeldByCurrentThread();
            }
        };
    }
}
