package com.hoangphuc.ddd.domain.service;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.model.vo.SaleWindow;

import java.time.LocalDateTime;

public interface TicketDetailDomainService {
    TicketDetail getTicketDetailById(Long ticketId);
    int getStockAvailable(Long ticketId);
    boolean decreaseStock(Long ticketId, int quantity);
    boolean increaseStock(Long ticketId, int quantity);

    /** Dot mo ban dang co hieu luc tai thoi diem "now". */
    SaleWindow resolveSaleWindow(LocalDateTime now);
}
