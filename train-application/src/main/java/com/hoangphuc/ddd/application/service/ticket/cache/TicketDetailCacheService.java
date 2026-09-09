package com.hoangphuc.ddd.application.service.ticket.cache;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.service.TicketDetailDomainService;
import com.hoangphuc.ddd.infrastructure.cache.redis.RedisInfrasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * TUYẾN PHÒNG THỦ THỨ HAI — chặn request trước khi nó chạm MySQL.
 *
 * Mẫu Cache-Aside (còn gọi là Lazy Loading):
 *   1. Hỏi cache trước
 *   2. Có   -> trả luôn, KHÔNG đụng DB
 *   3. Không -> xuống DB, rồi ghi ngược lên cache cho lần sau
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketDetailCacheService {

    /** Cache sống 10 phút rồi tự chết. KHÔNG BAO GIỜ cache vĩnh viễn. */
    private static final Duration TTL = Duration.ofMinutes(10);

    private final RedisInfrasService redisInfrasService;
    private final TicketDetailDomainService ticketDetailDomainService;

    public TicketDetail getTicketDetail(Long ticketId) {
        String key = genKey(ticketId);

        // ---- BƯỚC 1: hỏi Redis ----
        TicketDetail cached = redisInfrasService.getObject(key, TicketDetail.class);
        if (cached != null) {
            log.info("[CACHE] HIT  | key={} -> KHONG dung MySQL", key);
            return cached;
        }

        // ---- BƯỚC 2: cache miss -> xuống MySQL ----
        log.info("[CACHE] MISS | key={} -> xuong MySQL", key);
        TicketDetail fromDb = ticketDetailDomainService.getTicketDetailById(ticketId);
        if (fromDb == null) {
            // Không tìm thấy thì KHÔNG cache null.
            // (Cache null để chống cache-penetration là bài riêng, chưa làm ở đây.)
            return null;
        }

        // ---- BƯỚC 3: ghi ngược lên Redis cho lần sau ----
        redisInfrasService.setObject(key, fromDb, TTL);
        log.info("[CACHE] FILL | key={} ttl={}phut", key, TTL.toMinutes());

        return fromDb;
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
}
