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
 * SECOND LINE OF DEFENCE for WRITES — keeps the stock count in Redis.
 *
 * Why separate from TicketDetailCacheService?
 *   - Ticket detail (name, price, description): almost never changes -> cache the whole object, 10 minute TTL
 *   - Stock: changes after EVERY purchase                             -> own key, numeric, updated with Lua
 * Mixing them would mean re-serializing the whole object on every purchase — slow and error-prone.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class StockCacheService {

    // ---------------------------------------------------------------
    // DEDUCT STOCK — atomically.
    // Redis is single-threaded and treats the whole script as ONE command,
    // so no thread can cut in between the GET and the SET.
    //   -1 = key does not exist (not warmed up)
    //    0 = not enough stock
    //    1 = deducted
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

    // RESTORE STOCK — used when the DB fails after Redis has deducted (compensation)
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
     * Load the stock from MySQL into Redis. Called at startup, or on a cache miss.
     */
    public boolean warmUp(Long ticketId) {
        if (ticketId == null) {
            return false;
        }
        TicketDetail detail = ticketDetailDomainService.getTicketDetailById(ticketId);
        if (detail == null) {
            log.warn("[STOCK] warmUp: ticket not found ticketId={}", ticketId);
            return false;
        }
        redisInfrasService.setInt(genKey(ticketId), detail.getStockAvailable());
        log.info("[STOCK] warmUp | key={} stock={}", genKey(ticketId), detail.getStockAvailable());
        return true;
    }

    /**
     * Deduct stock in Redis, atomically.
     * @return 1 = OK | 0 = sold out | -1 = key not in Redis yet
     */
    public int deduct(Long ticketId, int quantity) {
        Long result = redisInfrasService.executeScript(
                SCRIPT_DEDUCT,
                Collections.singletonList(genKey(ticketId)),
                quantity);
        return result == null ? -1 : result.intValue();
    }

    /**
     * Put stock back into Redis. Called when Redis was deducted but a later step failed.
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
