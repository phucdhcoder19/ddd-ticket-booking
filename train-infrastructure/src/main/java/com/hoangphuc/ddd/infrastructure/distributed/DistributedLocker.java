package com.hoangphuc.ddd.infrastructure.distributed;

import java.util.concurrent.TimeUnit;

/**
 * Một "ổ khoá" dùng chung giữa mọi server — khác với synchronized của Java
 * chỉ có tác dụng trong 1 JVM.
 *
 * Tầng trên chỉ thấy interface này, KHÔNG thấy Redisson.
 * Mai đổi sang ZooKeeper thì viết impl mới, code gọi không đổi.
 */
public interface DistributedLocker {

    /**
     * @param waitTime  chờ tối đa bao lâu để giành khoá; quá thì bỏ cuộc
     * @param leaseTime giữ khoá tối đa bao lâu; quá thì Redis TỰ XOÁ
     *                  -> server ôm khoá mà chết cũng không làm kẹt hệ thống
     * @return true nếu giành được
     */
    boolean tryLock(long waitTime, long leaseTime, TimeUnit unit) throws InterruptedException;

    /** Nhả khoá. Chỉ nhả nếu chính thread này đang giữ. */
    void unlock();

    boolean isHeldByCurrentThread();
}
