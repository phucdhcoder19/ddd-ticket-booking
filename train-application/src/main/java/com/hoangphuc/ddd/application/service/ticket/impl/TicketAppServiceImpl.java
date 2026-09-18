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
     * Luồng đặt vé:
     *
     *   ⓪ kiểm tra luật mở bán    <- chặn trước khi đụng kho
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

        // ===== ⓪ Cổng nghiệp vụ — chặn TRƯỚC khi đụng vào kho =====
        // FE có ẩn nút Mua, nhưng curl thì không chạy FE. Luật mở bán phải được
        // kiểm ở ĐÂY — nơi ra quyết định — chứ không phải ở tầng hiển thị.
        // Đọc vé lên đầu luôn: vừa để kiểm tra, vừa lấy giá -> không tốn thêm
        // lần đọc cache nào, và bớt được 1 đường phải restore() Redis.
        TicketDetail detail = ticketDetailCacheService.getTicketDetail(ticketId);
        if (detail == null || detail.effectivePrice() == null) {
            return PlaceOrderResult.fail(PlaceOrderResult.Status.TICKET_NOT_FOUND);
        }

        LocalDateTime now = LocalDateTime.now();
        if (!detail.isOpenedForSale(now)) {
            log.info("[APP] ve chua mo ban | ticketId={} status={} saleStart={}",
                    ticketId, detail.getStatus(), detail.getSaleStartTime());
            return PlaceOrderResult.fail(PlaceOrderResult.Status.NOT_ON_SALE);
        }
        if (detail.isSaleEnded(now)) {
            log.info("[APP] het gio ban | ticketId={} saleEnd={}", ticketId, detail.getSaleEndTime());
            return PlaceOrderResult.fail(PlaceOrderResult.Status.SALE_ENDED);
        }
        BigDecimal unitPrice = detail.effectivePrice();

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
