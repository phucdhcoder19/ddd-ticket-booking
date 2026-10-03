package com.hoangphuc.ddd.domain.service;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.model.vo.SaleWindow;

import java.time.LocalDateTime;

public interface TicketDetailDomainService {
    TicketDetail getTicketDetailById(Long ticketId);
    int getStockAvailable(Long ticketId);
    boolean decreaseStock(Long ticketId, int quantity);
    boolean increaseStock(Long ticketId, int quantity);

    /**
     * Sale window for DISPLAY: prefers the upcoming one, so the home page can count down.
     * NOT used as the purchase gate — see isSaleOpen().
     */
    SaleWindow resolveSaleWindow(LocalDateTime now);

    /** Whether a sale is open right now. This is the actual purchase gate. */
    boolean isSaleOpen(LocalDateTime now);
}
