package com.hoangphuc.ddd.infrastructure.distributed.redisson;

import com.hoangphuc.ddd.infrastructure.distributed.DistributedLockService;
import com.hoangphuc.ddd.infrastructure.distributed.DistributedLocker;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Bản cài đặt bằng Redisson. Đây là NƠI DUY NHẤT trong project
 * import org.redisson.* — mọi chỗ khác chỉ biết DistributedLocker.
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
                // CHỈ nhả khi chính thread này đang giữ.
                // Thiếu kiểm tra này, thread A có thể nhả nhầm khoá của thread B
                // (khi khoá của A đã hết leaseTime và B vừa giành được).
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
