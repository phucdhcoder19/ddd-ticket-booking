package com.hoangphuc.ddd.application.service.order.impl;

import com.hoangphuc.ddd.application.model.HoldItemDTO;
import com.hoangphuc.ddd.application.model.OrderDTO;
import com.hoangphuc.ddd.application.model.OrderResult;
import com.hoangphuc.ddd.application.service.order.OrderAppService;
import com.hoangphuc.ddd.application.service.pricing.Journey;
import com.hoangphuc.ddd.application.service.pricing.JourneyPricingService;
import com.hoangphuc.ddd.application.service.queue.QueueAppService;
import com.hoangphuc.ddd.application.service.ticket.OrderTransactionService;
import com.hoangphuc.ddd.application.service.ticket.PassengersMissingException;
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
    private final QueueAppService queueAppService;

    /**
     * Much shorter than createHold(), because the hard part was done when holding:
     *
     *   NO sale time check   — done when the hold was created
     *   NO seat claiming     — the seats already belong to this hold
     *   NO repricing         — prices are fixed on each passenger
     *
     * What is left: win the right to change the hold's status, check that all
     * passengers are there, then write the order and transfer the seats.
     */
    @Override
    public OrderResult createFromHold(String holdCode, String queueToken) {
        log.info("[ORDER] createFromHold | holdCode={}", holdCode);

        Optional<Hold> found = holdRepository.findByCode(holdCode);
        if (found.isEmpty()) {
            return OrderResult.fail(OrderResult.Status.HOLD_NOT_FOUND);
        }
        Hold hold = found.get();

        try {
            TicketOrder order = orderTransactionService.convertSeatHoldToOrder(hold);
            if (order == null) {
                // Lost the race with the job, or the hold was already used.
                // DO NOT return seats here — the winner took care of it, or the
                // seats still belong to an order. Touching them resells seats
                // that already have an owner.
                return OrderResult.fail(OrderResult.Status.HOLD_EXPIRED);
            }

            // Done buying: leave "inside" and give the slot to someone waiting
            // right away, instead of occupying it for the full 15 minutes. Done
            // AFTER the transaction COMMITs: freeing the slot first and then
            // failing the order would cost the customer both the slot and the
            // ticket. And a Redis error here must not break an order that
            // already succeeded — at worst the admission expires after 15 minutes.
            try {
                queueAppService.leave(queueToken);
            } catch (Exception e) {
                log.warn("[ORDER] could not free the waiting room slot | token={}", queueToken, e);
            }
            return OrderResult.success(toDTO(order));

        } catch (PassengersMissingException e) {
            // Rolled back: the hold is back to status 0, the seats stay with the customer.
            log.info("[ORDER] passengers missing | {}", e.getMessage());
            return OrderResult.fail(OrderResult.Status.PASSENGERS_MISSING);

        } catch (Exception e) {
            // The transaction has ROLLED BACK: hold back to status 0, no order,
            // seats still held. The customer still has time to try again.
            log.error("[ORDER] error creating order | holdCode={}", holdCode, e);
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
        // orderId and code are both the order number: the client needs one
        // value to look the order up and one to print for the customer, and
        // here they are the same. They are separate fields so the printed code
        // (VT2702061234) can change later without touching the lookup URL.
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
     * The order's seats, read back from the seat table via order_id.
     *
     * Buy-by-quantity orders (lessons 19/21) have no seats pointing at them,
     * so the list is empty — correct, not an omission.
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
