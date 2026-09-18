package com.hoangphuc.ddd.application.service.hold.impl;

import com.hoangphuc.ddd.application.model.HoldDTO;
import com.hoangphuc.ddd.application.model.HoldResult;
import com.hoangphuc.ddd.application.service.hold.HoldAppService;
import com.hoangphuc.ddd.application.service.hold.HoldTransactionService;
import com.hoangphuc.ddd.application.service.ticket.cache.StockCacheService;
import com.hoangphuc.ddd.application.service.ticket.cache.TicketDetailCacheService;
import com.hoangphuc.ddd.domain.model.entity.Hold;
import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.repository.HoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class HoldAppServiceImpl implements HoldAppService {

    private final TicketDetailCacheService ticketDetailCacheService;
    private final StockCacheService stockCacheService;
    private final HoldTransactionService holdTransactionService;
    private final HoldRepository holdRepository;

    /**
     * Giữ bao lâu. Kiểu Duration chứ không phải long phút, để cấu hình viết
     * được "10m" khi chạy thật và "30s" khi ngồi test job — Spring tự đổi
     * chuỗi sang Duration, không phải tự nhân chia.
     */
    @Value("${app.hold.duration:10m}")
    private Duration holdDuration;

    /** Mỗi lượt quét dọn tối đa bao nhiêu — giữ transaction ngắn. */
    @Value("${app.hold.release-batch:200}")
    private int releaseBatch;

    /**
     * Luồng giữ chỗ — cùng hình dạng với placeOrder():
     *
     *   ⓪ cổng nghiệp vụ    <- chưa đụng kho, thoát tự do
     *   ① Redis trừ kho     <- TỪ ĐÂY mọi đường thoát phải restore()
     *   ②③ MySQL 1 transaction: trừ kho + ghi hold
     */
    @Override
    public HoldResult createHold(Long ticketId, Long userId, int quantity) {
        log.info("[HOLD] createHold | ticketId={} userId={} qty={}", ticketId, userId, quantity);

        // ===== ⓪ Cổng nghiệp vụ =====
        TicketDetail detail = ticketDetailCacheService.getTicketDetail(ticketId);
        if (detail == null || detail.effectivePrice() == null) {
            return HoldResult.fail(HoldResult.Status.TICKET_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        if (!detail.isOpenedForSale(now)) {
            return HoldResult.fail(HoldResult.Status.NOT_ON_SALE);
        }
        if (detail.isSaleEnded(now)) {
            return HoldResult.fail(HoldResult.Status.SALE_ENDED);
        }
        BigDecimal unitPrice = detail.effectivePrice();

        // ===== ① Redis — chặn sớm =====
        int redisResult = stockCacheService.deduct(ticketId, quantity);
        if (redisResult == -1) {
            if (!stockCacheService.warmUp(ticketId)) {
                return HoldResult.fail(HoldResult.Status.TICKET_NOT_FOUND);
            }
            redisResult = stockCacheService.deduct(ticketId, quantity);
        }
        if (redisResult == 0) {
            return HoldResult.fail(HoldResult.Status.OUT_OF_STOCK);
        }
        // Redis ĐÃ trừ -> mọi đường thoát thất bại đều phải restore()

        // ===== 23 MySQL =====
        String holdCode = "HOLD-" + UUID.randomUUID().toString().substring(0, 12).toUpperCase();
        LocalDateTime expireAt = now.plus(holdDuration);
        try {
            Hold hold = holdTransactionService
                    .deductStockAndCreateHold(ticketId, userId, quantity, holdCode, expireAt);

            if (hold == null) {
                stockCacheService.restore(ticketId, quantity);
                log.warn("[HOLD] MySQL tu choi, da hoan Redis | ticketId={}", ticketId);
                return HoldResult.fail(HoldResult.Status.OUT_OF_STOCK);
            }

            ticketDetailCacheService.evict(ticketId);
            log.info("[HOLD] giu cho OK | holdCode={} het han luc {}", holdCode, expireAt);
            return HoldResult.success(toDTO(hold, detail, unitPrice, now));

        } catch (Exception e) {
            // Tới đây nghĩa là transaction ĐÃ ROLLBACK: kho MySQL về số cũ.
            // Chỉ còn Redis chưa hoàn -> hoàn tay.
            stockCacheService.restore(ticketId, quantity);
            log.error("[HOLD] loi, MySQL da rollback, da hoan Redis | ticketId={}", ticketId, e);
            return HoldResult.fail(HoldResult.Status.ERROR);
        }
    }

    @Override
    public HoldResult getHold(String holdCode) {
        Optional<Hold> found = holdRepository.findByCode(holdCode);
        if (found.isEmpty()) {
            return HoldResult.fail(HoldResult.Status.HOLD_NOT_FOUND);
        }
        Hold hold = found.get();
        LocalDateTime now = LocalDateTime.now();

        // Quá hạn nhưng job chưa kịp quét tới: trả lời theo SỰ THẬT ngay lúc
        // hỏi, đừng đợi job. Người dùng không cần biết job chạy mỗi 10 giây.
        if (!hold.isHolding(now)) {
            return HoldResult.fail(HoldResult.Status.HOLD_EXPIRED);
        }

        TicketDetail detail = ticketDetailCacheService.getTicketDetail(hold.getTicketId());
        BigDecimal unitPrice = detail != null ? detail.effectivePrice() : BigDecimal.ZERO;
        return HoldResult.success(toDTO(hold, detail, unitPrice, now));
    }

    @Override
    public HoldResult releaseHold(String holdCode) {
        Optional<Hold> found = holdRepository.findByCode(holdCode);
        if (found.isEmpty()) {
            return HoldResult.fail(HoldResult.Status.HOLD_NOT_FOUND);
        }
        Hold hold = found.get();

        boolean released = holdTransactionService.releaseOne(hold);
        if (!released) {
            // 0 dòng: job vừa dọn xong, hoặc đã đổi thành đơn hàng.
            return HoldResult.fail(HoldResult.Status.HOLD_EXPIRED);
        }
        restoreCache(hold);

        log.info("[HOLD] user huy | holdCode={}", holdCode);
        return HoldResult.success(null);
    }

    @Override
    public int releaseExpiredHolds() {
        List<Hold> expired = holdRepository.findExpired(LocalDateTime.now(), releaseBatch);
        int count = 0;
        for (Hold hold : expired) {
            try {
                if (holdTransactionService.releaseOne(hold)) {
                    restoreCache(hold);
                    count++;
                    log.info("[HOLD-JOB] thu hoi | holdCode={} ticketId={} qty={}",
                            hold.getHoldCode(), hold.getTicketId(), hold.getQuantity());
                }
            } catch (Exception e) {
                // Một lượt lỗi không được làm chết cả lô — lượt quét sau thử lại.
                log.error("[HOLD-JOB] loi khi thu hoi | holdCode={}", hold.getHoldCode(), e);
            }
        }
        return count;
    }

    /**
     * Hoàn Redis SAU KHI transaction MySQL đã commit.
     *
     * Không gộp vào trong transaction: nếu transaction rollback sau đó thì
     * Redis đã cộng rồi, không rollback theo được -> Redis nhiều hơn MySQL
     * -> bán quá số vé thật.
     */
    private void restoreCache(Hold hold) {
        stockCacheService.restore(hold.getTicketId(), hold.getQuantity());
        ticketDetailCacheService.evict(hold.getTicketId());
    }

    private HoldDTO toDTO(Hold hold, TicketDetail detail, BigDecimal unitPrice, LocalDateTime now) {
        HoldDTO dto = new HoldDTO();
        dto.setHoldCode(hold.getHoldCode());
        dto.setTicketId(hold.getTicketId());
        dto.setTicketName(detail != null ? detail.getName() : null);
        dto.setQuantity(hold.getQuantity());
        dto.setUnitPrice(unitPrice);
        dto.setTotalAmount(unitPrice.multiply(BigDecimal.valueOf(hold.getQuantity())));
        dto.setExpiresAt(hold.getExpireAt());
        dto.setServerNow(now);
        dto.setSecondsLeft(hold.secondsLeft(now));
        dto.setStatus(hold.isHolding(now) ? "HOLDING" : "EXPIRED");
        return dto;
    }
}
