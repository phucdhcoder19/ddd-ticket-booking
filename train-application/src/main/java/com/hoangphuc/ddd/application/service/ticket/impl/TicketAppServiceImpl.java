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
     * Luồng đặt vé:
     *
     *   ① Redis Lua trừ kho          <- ngoài transaction, chặn sớm
     *   ┌───────── 1 transaction MySQL ─────────┐
     *   │ ② trừ kho MySQL                        │
     *   │ ③ tạo đơn                              │
     *   └────────────────────────────────────────┘
     *   Lỗi ở ②③ -> MySQL tự ROLLBACK, code hoàn tay Redis ①
     */
    @Override
    public PlaceOrderResult placeOrder(Long ticketId, Long userId, int quantity) {
        log.info("[APP] placeOrder | ticketId={} userId={} qty={}", ticketId, userId, quantity);

        // ===== ① Redis Lua — tuyến phòng thủ 2 =====
        int redisResult = stockCacheService.deduct(ticketId, quantity);
        if (redisResult == -1) {
            if (!stockCacheService.warmUp(ticketId)) {
                return PlaceOrderResult.fail(PlaceOrderResult.Status.TICKET_NOT_FOUND);
            }
            redisResult = stockCacheService.deduct(ticketId, quantity);
        }
        if (redisResult == 0) {
            log.info("[APP] het ve (chan o Redis) | ticketId={}", ticketId);
            return PlaceOrderResult.fail(PlaceOrderResult.Status.OUT_OF_STOCK);
        }
        // Từ đây Redis ĐÃ trừ -> mọi đường thoát thất bại đều phải restore()

        // Lấy giá từ cache (giá hiếm khi đổi, không cần đọc MySQL)
        TicketDetail detail = ticketDetailCacheService.getTicketDetail(ticketId);
        if (detail == null || detail.effectivePrice() == null) {
            stockCacheService.restore(ticketId, quantity);
            return PlaceOrderResult.fail(PlaceOrderResult.Status.TICKET_NOT_FOUND);
        }
        BigDecimal unitPrice = detail.effectivePrice();

        // ===== ②③ MySQL: trừ kho + tạo đơn trong 1 transaction =====
        try {
            TicketOrder order = orderTransactionService
                    .deductStockAndCreateOrder(ticketId, userId, quantity, unitPrice);

            if (order == null) {
                // MySQL không đủ vé (Redis lệch) -> hoàn Redis
                stockCacheService.restore(ticketId, quantity);
                log.warn("[APP] MySQL tu choi, da hoan Redis | ticketId={}", ticketId);
                return PlaceOrderResult.fail(PlaceOrderResult.Status.OUT_OF_STOCK);
            }

            // Kho đã đổi -> xoá cache chi tiết vé cũ
            ticketDetailCacheService.evict(ticketId);

            log.info("[APP] placeOrder OK | orderNumber={}", order.getOrderNumber());
            return PlaceOrderResult.success(order.getOrderNumber(), order.getQuantity(), order.getTotalAmount());

        } catch (Exception e) {
            // Tới được đây nghĩa là transaction ĐÃ ROLLBACK: kho MySQL về số cũ, không có đơn.
            // Chỉ còn Redis là chưa được hoàn -> hoàn tay.
            stockCacheService.restore(ticketId, quantity);
            log.error("[APP] placeOrder loi, MySQL da rollback, da hoan Redis | ticketId={}", ticketId, e);
            return PlaceOrderResult.fail(PlaceOrderResult.Status.ERROR);
        }
    }
}
