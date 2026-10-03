package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TicketDetailRepository {

    TicketDetail findById(Long ticketId);

    int getStockAvailable(Long ticketId);

    /** The nearest active ticket whose sale has NOT opened yet. */
    Optional<TicketDetail> findNextOpening(LocalDateTime now);

    /** The active ticket that opened most recently (it may already be past its sale window). */
    Optional<TicketDetail> findLatestOpened(LocalDateTime now);

    /** First line of defence: only deduct when there is enough stock. Returns true if deducted. */
    boolean decreaseStock(Long ticketId, int quantity);

    boolean increaseStock(Long ticketId, int quantity);
}
