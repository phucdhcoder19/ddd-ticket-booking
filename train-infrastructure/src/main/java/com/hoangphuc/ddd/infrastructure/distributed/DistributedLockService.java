package com.hoangphuc.ddd.infrastructure.distributed;

/**
 * Nơi lấy ổ khoá. Mỗi lockKey là một ổ khoá riêng.
 * Ví dụ "lock:ticket:detail:1" và "lock:ticket:detail:2" không chặn nhau.
 */
public interface DistributedLockService {

    DistributedLocker getLock(String lockKey);
}
