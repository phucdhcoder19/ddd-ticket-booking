package com.hoangphuc.ddd.infrastructure.distributed;

import java.util.concurrent.TimeUnit;

/**
 * A lock shared by every server — unlike Java's synchronized, which only
 * works inside one JVM.
 *
 * The layers above only see this interface, NOT Redisson.
 * Switching to ZooKeeper later means writing a new impl; callers do not change.
 */
public interface DistributedLocker {

    /**
     * @param waitTime  how long to wait for the lock at most; give up after that
     * @param leaseTime how long the lock may be held at most; after that Redis DELETES it
     *                  -> a server that dies while holding the lock cannot freeze the system
     * @return true if the lock was acquired
     */
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

    /** Release the lock. Only releases it if the current thread holds it. */
    void unlock();

    boolean isHeldByCurrentThread();
}
