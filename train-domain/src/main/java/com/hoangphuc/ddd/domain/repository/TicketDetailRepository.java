package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TicketDetailRepository {

    TicketDetail findById(Long ticketId);

    int getStockAvailable(Long ticketId);

    /** Ve dang ban gan nhat CHUA toi gio mo. */
    Optional<TicketDetail> findNextOpening(LocalDateTime now);

    /** Ve dang ban co gio mo gan day nhat (chua chac con trong khung ban). */
    Optional<TicketDetail> findLatestOpened(LocalDateTime now);

    /** Tuyến phòng thủ 1: chỉ trừ khi còn đủ vé. Trả về true nếu trừ được. */
    boolean decreaseStock(Long ticketId, int quantity);

    boolean increaseStock(Long ticketId, int quantity);
}
