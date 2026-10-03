package com.hoangphuc.ddd.infrastructure.distributed;

/**
 * Where locks come from. Each lockKey is a separate lock.
 * For example "lock:ticket:detail:1" and "lock:ticket:detail:2" do not block each other.
 */
public interface DistributedLockService {

    DistributedLocker getLock(String lockKey);
}
