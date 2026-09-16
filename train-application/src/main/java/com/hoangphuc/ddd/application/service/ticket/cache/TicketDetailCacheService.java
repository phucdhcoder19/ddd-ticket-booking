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
 * TUYẾN PHÒNG THỦ THỨ HAI — chặn request trước khi nó chạm MySQL.
 *
 * Mẫu Cache-Aside + Redisson lock chống CACHE STAMPEDE:
 *   1. Hỏi cache
 *   2. Trống -> giành khoá; chỉ 1 thread được xuống MySQL
 *   3. Sau khi có khoá, HỎI CACHE LẦN NỮA (double-check)
 *   4. Nạp từ MySQL, ghi cache, nhả khoá
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketDetailCacheService {

    /** Cache sống 10 phút rồi tự chết. KHÔNG BAO GIỜ cache vĩnh viễn. */
    private static final Duration TTL = Duration.ofMinutes(10);

    /** Chờ tối đa 1s để giành khoá. Quá thì bỏ cuộc, không xếp hàng vô tận. */
    private static final long LOCK_WAIT_SECONDS = 1;

    /** Giữ khoá tối đa 5s. Server chết giữa chừng thì Redis tự xoá khoá. */
    private static final long LOCK_LEASE_SECONDS = 5;

    private final RedisInfrasService redisInfrasService;
    private final TicketDetailDomainService ticketDetailDomainService;
    private final DistributedLockService distributedLockService;

    public TicketDetail getTicketDetail(Long ticketId) {
        String key = genKey(ticketId);

        // ---- BƯỚC 1: hỏi Redis ----
        TicketDetail cached = redisInfrasService.getObject(key, TicketDetail.class);
        if (cached != null) {
            log.info("[CACHE] HIT  | key={} -> KHONG dung MySQL", key);
            return cached;
        }

        log.info("[CACHE] MISS | key={} -> can khoa de xuong MySQL", key);

        // ---- BƯỚC 2: giành khoá ----
        // Không có bước này: 5000 thread cùng thấy cache trống ở BƯỚC 1
        // và cùng lao xuống MySQL -> cache stampede -> DB nghen.
        DistributedLocker locker = distributedLockService.getLock(genLockKey(ticketId));
        boolean locked = false;
        try {
            locked = locker.tryLock(LOCK_WAIT_SECONDS, LOCK_LEASE_SECONDS, TimeUnit.SECONDS);

            if (!locked) {
                // Người khác đang nạp. Chờ 1s rồi mà vẫn chưa xong -> thử đọc cache lần cuối.
                log.warn("[LOCK] khong gianh duoc khoa | key={} -> doc lai cache", key);
                return redisInfrasService.getObject(key, TicketDetail.class);
            }

            // ---- BƯỚC 3: DOUBLE-CHECK — bước quan trọng nhất ----
            // Thread trước có thể đã nạp xong trong lúc mình đứng chờ.
            // Thiếu bước này thì 5000 thread vẫn lần lượt xuống MySQL,
            // chỉ khác là xếp hàng thay vì ùa cùng lúc.
            cached = redisInfrasService.getObject(key, TicketDetail.class);
            if (cached != null) {
                log.info("[CACHE] HIT sau khi cho khoa | key={} -> KHONG dung MySQL", key);
                return cached;
            }

            // ---- BƯỚC 4: chỉ MỘT thread tới được đây ----
            log.info("[CACHE] nap tu MySQL | key={}", key);
            TicketDetail fromDb = ticketDetailDomainService.getTicketDetailById(ticketId);
            if (fromDb == null) {
                return null;
            }

            redisInfrasService.setObject(key, fromDb, TTL);
            log.info("[CACHE] FILL | key={} ttl={}phut", key, TTL.toMinutes());
            return fromDb;

        } catch (InterruptedException e) {
            // Thread bị ngắt trong lúc chờ khoá -> khôi phục cờ ngắt rồi thoát
            Thread.currentThread().interrupt();
            log.warn("[LOCK] bi ngat khi cho khoa | key={}", key);
            return null;

        } finally {
            // BẮT BUỘC nhả khoá trong finally — thiếu là khoá kẹt tới khi hết leaseTime,
            // mọi request cho vé này đứng im 5 giây.
            if (locked) {
                locker.unlock();
            }
        }
    }

    /**
     * Xoá cache khi dữ liệu gốc đổi (mua vé, sửa vé...).
     * Xoá chứ không cập nhật — đơn giản hơn và ít sai hơn.
     */
    public void evict(Long ticketId) {
        String key = genKey(ticketId);
        redisInfrasService.delete(key);
        log.info("[CACHE] EVICT| key={}", key);
    }

    private String genKey(Long ticketId) {
        return "TICKET:DETAIL:" + ticketId;
    }

    /** Khoá phải là key RIÊNG, không trùng key cache. */
    private String genLockKey(Long ticketId) {
        return "LOCK:TICKET:DETAIL:" + ticketId;
    }
}
