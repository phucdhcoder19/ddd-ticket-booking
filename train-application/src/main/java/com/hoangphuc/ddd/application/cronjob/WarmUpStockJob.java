package com.hoangphuc.ddd.application.cronjob;

import com.hoangphuc.ddd.application.service.ticket.cache.StockCacheService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Nạp sẵn tồn kho lên Redis khi app khởi động.
 *
 * Không có bước này thì request ĐẦU TIÊN của mỗi vé sẽ gặp -1 (cache miss)
 * rồi mới warm-up — đúng lúc 20h00 flash sale mở là hàng nghìn request
 * cùng miss một lúc. Nạp trước thì Redis đã sẵn sàng từ trước giờ G.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class WarmUpStockJob {

    private final StockCacheService stockCacheService;

    // Tạm hardcode id vé để học. Thực tế sẽ SELECT các vé sắp mở bán.
    private static final Long[] TICKET_IDS = { 1L };

    @PostConstruct
    public void warmUpOnStartup() {
        for (Long ticketId : TICKET_IDS) {
            boolean ok = stockCacheService.warmUp(ticketId);
            log.info("[WARMUP] ticketId={} ket qua={}", ticketId, ok);
        }
    }
}
