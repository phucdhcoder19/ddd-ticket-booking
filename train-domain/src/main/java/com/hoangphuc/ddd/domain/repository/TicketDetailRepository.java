package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;

public interface TicketDetailRepository {

    TicketDetail findById(Long ticketId);

    int getStockAvailable(Long ticketId);

    /** Tuyến phòng thủ 1: chỉ trừ khi còn đủ vé. Trả về true nếu trừ được. */
    boolean decreaseStock(Long ticketId, int quantity);

    boolean increaseStock(Long ticketId, int quantity);
}
