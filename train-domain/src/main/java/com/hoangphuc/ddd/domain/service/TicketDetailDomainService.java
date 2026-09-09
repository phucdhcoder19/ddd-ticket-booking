package com.hoangphuc.ddd.domain.service;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;

public interface TicketDetailDomainService {
    TicketDetail getTicketDetailById(Long ticketId);
    int getStockAvailable(Long ticketId);
    boolean decreaseStock(Long ticketId, int quantity);
    boolean increaseStock(Long ticketId, int quantity);
}
