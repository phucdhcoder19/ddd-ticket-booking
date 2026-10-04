package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.TicketOrder;

import java.util.List;

public interface TicketOrderRepository {

    TicketOrder save(TicketOrder order);

    TicketOrder findByOrderNumber(String orderNumber);

    /**
     * A user's seat-booking orders, newest first, at most `limit`.
     * Buy-by-quantity orders (no tripId) are left out: they have no seats or passengers to show.
     */
    List<TicketOrder> findSeatOrdersByUser(Long userId, int limit);
}
