package com.hoangphuc.ddd.application.service.ticket.cache;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.service.TicketDetailDomainService;
import com.hoangphuc.ddd.infrastructure.cache.redis.RedisInfrasService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * TUYẾN PHÒNG THỦ THỨ HAI cho việc GHI — giữ tồn kho trong Redis.
 *
 * Vì sao tách khỏi TicketDetailCacheService?
 *   - Chi tiết vé (tên, giá, mô tả): gần như không đổi  -> cache cả object, TTL 10 phút
 *   - Tồn kho: đổi sau MỖI lần mua                      -> key riêng, kiểu số, sửa bằng Lua
 * Trộn chung thì mỗi lần mua phải serialize lại cả object — vừa chậm vừa dễ sai.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockCacheService {

    // ---------------------------------------------------------------
    // TRỪ KHO — nguyên tử.
    // Redis chạy đơn luồng và coi cả script này là MỘT lệnh,
    // nên không thread nào chen được vào giữa GET và SET.
    //   -1 = key không tồn tại (chưa warm-up)
    //    0 = không đủ vé
    //    1 = trừ thành công
    // ---------------------------------------------------------------
    private static final String LUA_DEDUCT =
            "local stock = redis.call('GET', KEYS[1]); " +
            "if stock == false then return -1 end; " +
            "stock = tonumber(stock); " +
            "if (stock >= tonumber(ARGV[1])) then " +
            "   redis.call('SET', KEYS[1], stock - tonumber(ARGV[1])); " +
            "   return 1; " +
            "end; " +
            "return 0; ";

    // HOÀN KHO — dùng khi DB fail sau khi Redis đã trừ (bù trừ / compensation)
    private static final String LUA_RESTORE =
            "local stock = redis.call('GET', KEYS[1]); " +
            "if (stock) then " +
            "   redis.call('SET', KEYS[1], tonumber(stock) + tonumber(ARGV[1])); " +
            "   return 1; " +
            "end; " +
            "return 0; ";

    private static final DefaultRedisScript<Long> SCRIPT_DEDUCT =
            new DefaultRedisScript<>(LUA_DEDUCT, Long.class);

    private static final DefaultRedisScript<Long> SCRIPT_RESTORE =
            new DefaultRedisScript<>(LUA_RESTORE, Long.class);

    private final RedisInfrasService redisInfrasService;
    private final TicketDetailDomainService ticketDetailDomainService;

    /**
     * Nạp tồn kho từ MySQL lên Redis. Gọi lúc khởi động, hoặc khi phát hiện cache miss.
     */
    public boolean warmUp(Long ticketId) {
        if (ticketId == null) {
            return false;
        }
        TicketDetail detail = ticketDetailDomainService.getTicketDetailById(ticketId);
        if (detail == null) {
            log.warn("[STOCK] warmUp: khong tim thay ticketId={}", ticketId);
            return false;
        }
        redisInfrasService.setInt(genKey(ticketId), detail.getStockAvailable());
        log.info("[STOCK] warmUp | key={} stock={}", genKey(ticketId), detail.getStockAvailable());
        return true;
    }

    /**
     * Trừ kho trong Redis, nguyên tử.
     * @return 1 = OK | 0 = hết vé | -1 = chưa có key trong Redis
     */
    public int deduct(Long ticketId, int quantity) {
        Long result = redisInfrasService.executeScript(
                SCRIPT_DEDUCT,
                Collections.singletonList(genKey(ticketId)),
                quantity);
        return result == null ? -1 : result.intValue();
    }

    /**
     * Hoàn kho vào Redis. Gọi khi đã trừ Redis nhưng bước sau thất bại.
     */
    public void restore(Long ticketId, int quantity) {
        redisInfrasService.executeScript(
                SCRIPT_RESTORE,
                Collections.singletonList(genKey(ticketId)),
                quantity);
        log.info("[STOCK] restore | ticketId={} qty={}", ticketId, quantity);
    }

    public int currentStock(Long ticketId) {
        return redisInfrasService.getInt(genKey(ticketId));
    }

    private String genKey(Long ticketId) {
        return "TICKET:" + ticketId + ":STOCK";
    }
}
