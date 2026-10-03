package com.hoangphuc.ddd.application.cronjob;

import com.hoangphuc.ddd.application.service.ticket.cache.StockCacheService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Preloads stock into Redis when the app starts.
 *
 * Without this step, the FIRST request for each ticket hits -1 (cache miss)
 * and only then warms up — right at 8 PM when the flash sale opens, thousands
 * of requests miss at the same moment. Preloading means Redis is ready before
 * the sale starts.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WarmUpStockJob {

    private final StockCacheService stockCacheService;

    // Ticket id hardcoded for learning. In practice, SELECT the tickets about to go on sale.
    private static final Long[] TICKET_IDS = { 1L };

    @PostConstruct
    public void warmUpOnStartup() {
        for (Long ticketId : TICKET_IDS) {
            boolean ok = stockCacheService.warmUp(ticketId);
            log.info("[WARMUP] ticketId={} result={}", ticketId, ok);
        }
    }
}
