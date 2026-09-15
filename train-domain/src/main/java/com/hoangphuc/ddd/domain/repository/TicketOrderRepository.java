package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.TicketOrder;

public interface TicketOrderRepository {

    TicketOrder save(TicketOrder order);

    TicketOrder findByOrderNumber(String orderNumber);
}
