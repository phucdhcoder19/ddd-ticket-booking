package com.hoangphuc.ddd.application.service.order.impl;

import com.hoangphuc.ddd.application.model.OrderDTO;
import com.hoangphuc.ddd.application.model.OrderResult;
import com.hoangphuc.ddd.application.service.order.OrderAppService;
import com.hoangphuc.ddd.application.service.ticket.OrderTransactionService;
import com.hoangphuc.ddd.application.service.ticket.cache.TicketDetailCacheService;
import com.hoangphuc.ddd.domain.model.entity.Hold;
import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.model.entity.TicketOrder;
import com.hoangphuc.ddd.domain.repository.HoldRepository;
import com.hoangphuc.ddd.domain.repository.TicketOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderAppServiceImpl implements OrderAppService {

    private final HoldRepository holdRepository;
    private final TicketOrderRepository ticketOrderRepository;
    private final TicketDetailCacheService ticketDetailCacheService;
    private final OrderTransactionService orderTransactionService;

    /**
     * Luồng ngắn hơn hẳn createHold(), vì phần khó đã làm xong ở bước giữ chỗ:
     *
     *   KHÔNG kiểm giờ mở bán  — đã kiểm lúc tạo hold
     *   KHÔNG trừ kho Redis    — đã trừ lúc tạo hold
     *   KHÔNG trừ kho MySQL    — đã trừ lúc tạo hold
     *
     * Chỉ còn: giành quyền đổi trạng thái hold, rồi ghi đơn.
     */
    @Override
    public OrderResult createFromHold(String holdCode) {
        log.info("[ORDER] createFromHold | holdCode={}", holdCode);

        Optional<Hold> found = holdRepository.findByCode(holdCode);
        if (found.isEmpty()) {
            return OrderResult.fail(OrderResult.Status.HOLD_NOT_FOUND);
        }
        Hold hold = found.get();

        TicketDetail detail = ticketDetailCacheService.getTicketDetail(hold.getTicketId());
        if (detail == null || detail.effectivePrice() == null) {
            return OrderResult.fail(OrderResult.Status.TICKET_NOT_FOUND);
        }
        // Giá CHỐT TẠI ĐÂY và lưu vào đơn. Vé đổi giá sau này không làm đổi
        // số tiền của đơn đã tạo.
        BigDecimal unitPrice = detail.effectivePrice();

        try {
            TicketOrder order = orderTransactionService.convertHoldToOrder(hold, unitPrice);
            if (order == null) {
                // Thua cuộc đua với job, hoặc hold đã dùng rồi.
                // KHÔNG hoàn kho ở đây — bên thắng đã lo, hoặc kho vẫn đang
                // thuộc về đơn hàng. Đụng vào là cộng khống.
                return OrderResult.fail(OrderResult.Status.HOLD_EXPIRED);
            }
            return OrderResult.success(toDTO(order, detail));

        } catch (Exception e) {
            // Transaction đã ROLLBACK: hold về status 0, không có đơn.
            // Kho không đổi nên không phải dọn gì.
            log.error("[ORDER] loi khi tao don | holdCode={}", holdCode, e);
            return OrderResult.fail(OrderResult.Status.ERROR);
        }
    }

    @Override
    public OrderResult getByOrderNumber(String orderNumber) {
        TicketOrder order = ticketOrderRepository.findByOrderNumber(orderNumber);
        if (order == null) {
            return OrderResult.fail(OrderResult.Status.HOLD_NOT_FOUND);
        }
        TicketDetail detail = ticketDetailCacheService.getTicketDetail(order.getTicketId());
        return OrderResult.success(toDTO(order, detail));
    }

    private OrderDTO toDTO(TicketOrder order, TicketDetail detail) {
        OrderDTO dto = new OrderDTO();
        dto.setOrderNumber(order.getOrderNumber());
        dto.setTicketId(order.getTicketId());
        dto.setTicketName(detail != null ? detail.getName() : null);
        dto.setQuantity(order.getQuantity());
        dto.setUnitPrice(order.getUnitPrice());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(statusLabel(order.getOrderStatus()));
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }

    private String statusLabel(int status) {
        return switch (status) {
            case TicketOrder.STATUS_PAID -> "PAID";
            case TicketOrder.STATUS_CANCELLED -> "CANCELLED";
            default -> "PENDING";
        };
    }
}
