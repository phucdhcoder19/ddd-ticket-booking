package com.hoangphuc.ddd.application.service.order.impl;

import com.hoangphuc.ddd.application.model.HoldItemDTO;
import com.hoangphuc.ddd.application.model.OrderDTO;
import com.hoangphuc.ddd.application.model.OrderResult;
import com.hoangphuc.ddd.application.service.order.OrderAppService;
import com.hoangphuc.ddd.application.service.pricing.Journey;
import com.hoangphuc.ddd.application.service.pricing.JourneyPricingService;
import com.hoangphuc.ddd.application.service.ticket.OrderTransactionService;
import com.hoangphuc.ddd.domain.model.entity.Hold;
import com.hoangphuc.ddd.domain.model.entity.Seat;
import com.hoangphuc.ddd.domain.model.entity.TicketOrder;
import com.hoangphuc.ddd.domain.repository.HoldRepository;
import com.hoangphuc.ddd.domain.repository.SeatRepository;
import com.hoangphuc.ddd.domain.repository.TicketOrderRepository;
import com.hoangphuc.ddd.domain.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderAppServiceImpl implements OrderAppService {

    private final HoldRepository holdRepository;
    private final SeatRepository seatRepository;
    private final TripRepository tripRepository;
    private final TicketOrderRepository ticketOrderRepository;
    private final OrderTransactionService orderTransactionService;
    private final JourneyPricingService journeyPricingService;

    /**
     * Luồng ngắn hơn hẳn createHold(), vì phần khó đã làm xong ở bước giữ chỗ:
     *
     *   KHÔNG kiểm giờ mở bán  — đã kiểm lúc tạo hold
     *   KHÔNG giành ghế        — ghế đã thuộc về hold này rồi
     *   KHÔNG tính lại tiền    — giá đã chốt và lưu trên hold
     *
     * Chỉ còn: giành quyền đổi trạng thái hold, rồi ghi đơn và sang tên ghế.
     */
    @Override
    public OrderResult createFromHold(String holdCode) {
        log.info("[ORDER] createFromHold | holdCode={}", holdCode);

        Optional<Hold> found = holdRepository.findByCode(holdCode);
        if (found.isEmpty()) {
            return OrderResult.fail(OrderResult.Status.HOLD_NOT_FOUND);
        }
        Hold hold = found.get();

        try {
            TicketOrder order = orderTransactionService.convertSeatHoldToOrder(hold);
            if (order == null) {
                // Thua cuộc đua với job, hoặc hold đã dùng rồi.
                // KHÔNG trả ghế ở đây — bên thắng đã lo, hoặc ghế vẫn đang
                // thuộc về đơn hàng. Đụng vào là bán lại chỗ đã có chủ.
                return OrderResult.fail(OrderResult.Status.HOLD_EXPIRED);
            }
            return OrderResult.success(toDTO(order));

        } catch (Exception e) {
            // Transaction đã ROLLBACK: hold về status 0, không có đơn, ghế
            // vẫn đang được giữ. Khách còn thời gian bấm lại.
            log.error("[ORDER] loi khi tao don | holdCode={}", holdCode, e);
            return OrderResult.fail(OrderResult.Status.ERROR);
        }
    }

    @Override
    public OrderResult getByOrderNumber(String orderNumber) {
        TicketOrder order = ticketOrderRepository.findByOrderNumber(orderNumber);
        if (order == null) {
            return OrderResult.fail(OrderResult.Status.ORDER_NOT_FOUND);
        }
        return OrderResult.success(toDTO(order));
    }

    private OrderDTO toDTO(TicketOrder order) {
        OrderDTO dto = new OrderDTO();
        // orderId và code cùng là mã đơn: client cần một thứ để tra cứu và
        // một thứ để in cho khách đọc, và ở đây chúng là một. Tách sẵn hai
        // trường để sau này đổi mã in (VT2702061234) mà không phải sửa
        // đường dẫn tra cứu.
        dto.setOrderId(order.getOrderNumber());
        dto.setCode(order.getOrderNumber());
        dto.setStatus(statusLabel(order.getOrderStatus()));
        dto.setTotalAmount(order.getTotalAmount() != null
                ? order.getTotalAmount().setScale(0, RoundingMode.HALF_UP).longValue()
                : 0L);
        dto.setTripId(order.getTripId() != null ? String.valueOf(order.getTripId()) : null);
        dto.setFromCode(order.getFromCode());
        dto.setToCode(order.getToCode());
        dto.setItems(itemsOf(order));
        dto.setPaidAt(order.getPaidAt());
        dto.setCreatedAt(order.getCreatedAt());
        return dto;
    }

    /**
     * Những chỗ của đơn, đọc ngược từ bảng seat qua cột order_id.
     *
     * Đơn mua thẳng (luồng bài 19/21) không có ghế nào trỏ về, nên trả danh
     * sách rỗng — đúng, không phải thiếu sót.
     */
    private List<HoldItemDTO> itemsOf(TicketOrder order) {
        List<Seat> seats = seatRepository.findByOrder(order.getId());
        if (seats.isEmpty()) {
            return List.of();
        }
        Journey journey = journeyOf(order);

        List<HoldItemDTO> items = new ArrayList<>(seats.size());
        for (Seat seat : seats) {
            HoldItemDTO item = new HoldItemDTO();
            item.setSeatId(seat.getSeatCode());
            item.setSeatLabel(seat.getLabel());
            item.setCarriageNumber(seat.getCarriageNumber());
            item.setSeatClass(seat.getSeatClass());
            item.setPrice(journey != null ? journeyPricingService.fareOf(journey, seat) : 0L);
            items.add(item);
        }
        return items;
    }

    private Journey journeyOf(TicketOrder order) {
        if (order.getTripId() == null) {
            return null;
        }
        LocalDate serviceDate = tripRepository.findById(order.getTripId())
                .map(trip -> trip.getServiceDate())
                .orElse(null);
        if (serviceDate == null) {
            return null;
        }
        return journeyPricingService
                .resolve(order.getFromCode(), order.getToCode(), serviceDate)
                .orElse(null);
    }

    private String statusLabel(int status) {
        return switch (status) {
            case TicketOrder.STATUS_PAID -> "PAID";
            case TicketOrder.STATUS_CANCELLED -> "CANCELLED";
            default -> "PENDING";
        };
    }
}
