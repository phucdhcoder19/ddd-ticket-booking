package com.hoangphuc.ddd.application.service.ticket.impl;

import com.hoangphuc.ddd.application.mapper.TicketMapper;
import com.hoangphuc.ddd.application.model.PlaceOrderResult;
import com.hoangphuc.ddd.application.model.TicketDetailDTO;
import com.hoangphuc.ddd.application.service.ticket.OrderTransactionService;
import com.hoangphuc.ddd.application.service.ticket.TicketAppService;
import com.hoangphuc.ddd.application.service.ticket.cache.StockCacheService;
import com.hoangphuc.ddd.application.service.ticket.cache.TicketDetailCacheService;
import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.model.entity.TicketOrder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketAppServiceImpl implements TicketAppService {

    private final TicketDetailCacheService ticketDetailCacheService;
    private final StockCacheService stockCacheService;
    private final OrderTransactionService orderTransactionService;

    @Override
    public TicketDetailDTO getTicketDetail(Long ticketId) {
        log.info("[APP] getTicketDetail | ticketId={}", ticketId);
        TicketDetail entity = ticketDetailCacheService.getTicketDetail(ticketId);
        return TicketMapper.toDTO(entity);
    }

    /**
     * Purchase flow:
     *
     *   ⓪ check the sale rules        <- reject before touching stock
     *   ① Redis Lua stock deduction   <- outside the transaction, rejects early
     *   ┌───────── 1 MySQL transaction ─────────┐
     *   │ ② deduct MySQL stock                   │
     *   │ ③ create the order                     │
     *   └────────────────────────────────────────┘
     *   Failure in ②③ -> MySQL ROLLS BACK by itself, the code restores Redis ① by hand
     */
    @Override
    public PlaceOrderResult placeOrder(Long ticketId, Long userId, int quantity) {
        log.info("[APP] placeOrder | ticketId={} userId={} qty={}", ticketId, userId, quantity);

        // ===== ⓪ Business gate — reject BEFORE touching stock =====
        // The frontend hides the Buy button, but curl does not run the frontend.
        // Sale rules must be checked HERE — where the decision is made — not in
        // the display layer. Reading the ticket first does double duty: it is
        // the check and it gives us the price -> no extra cache read, and one
        // less path that needs restore() on Redis.
        TicketDetail detail = ticketDetailCacheService.getTicketDetail(ticketId);
        if (detail == null || detail.effectivePrice() == null) {
            return PlaceOrderResult.fail(PlaceOrderResult.Status.TICKET_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!detail.isOpenedForSale(now)) {
            log.info("[APP] ticket not on sale yet | ticketId={} status={} saleStart={}",
                    ticketId, detail.getStatus(), detail.getSaleStartTime());
            return PlaceOrderResult.fail(PlaceOrderResult.Status.NOT_ON_SALE);
        }
        if (detail.isSaleEnded(now)) {
            log.info("[APP] sale ended | ticketId={} saleEnd={}", ticketId, detail.getSaleEndTime());
            return PlaceOrderResult.fail(PlaceOrderResult.Status.SALE_ENDED);
        }
        BigDecimal unitPrice = detail.effectivePrice();

        // ===== ① Redis Lua — second line of defence =====
        int redisResult = stockCacheService.deduct(ticketId, quantity);
        if (redisResult == -1) {
            if (!stockCacheService.warmUp(ticketId)) {
                return PlaceOrderResult.fail(PlaceOrderResult.Status.TICKET_NOT_FOUND);
            }
            redisResult = stockCacheService.deduct(ticketId, quantity);
        }
        if (redisResult == 0) {
            log.info("[APP] sold out (rejected at Redis) | ticketId={}", ticketId);
            return PlaceOrderResult.fail(PlaceOrderResult.Status.OUT_OF_STOCK);
        }
        // From here on Redis HAS deducted -> every failure exit must call restore()

        // ===== ②③ MySQL: deduct stock + create the order in 1 transaction =====
        try {
            TicketOrder order = orderTransactionService
                    .deductStockAndCreateOrder(ticketId, userId, quantity, unitPrice);

            if (order == null) {
                // MySQL does not have enough stock (Redis drifted) -> restore Redis
                stockCacheService.restore(ticketId, quantity);
                log.warn("[APP] MySQL rejected, Redis restored | ticketId={}", ticketId);
                return PlaceOrderResult.fail(PlaceOrderResult.Status.OUT_OF_STOCK);
            }

            // Stock changed -> drop the stale ticket detail cache
            ticketDetailCacheService.evict(ticketId);

            log.info("[APP] placeOrder OK | orderNumber={}", order.getOrderNumber());
            return PlaceOrderResult.success(order.getOrderNumber(), order.getQuantity(), order.getTotalAmount());

        } catch (Exception e) {
            // Getting here means the transaction HAS ROLLED BACK: MySQL stock is
            // back to its old value, no order exists. Only Redis is not restored
            // yet -> restore it by hand.
            stockCacheService.restore(ticketId, quantity);
            log.error("[APP] placeOrder failed, MySQL rolled back, Redis restored | ticketId={}", ticketId, e);
            return PlaceOrderResult.fail(PlaceOrderResult.Status.ERROR);
        }
    }
}
