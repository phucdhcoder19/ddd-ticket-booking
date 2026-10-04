package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.TicketOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketOrderJPAMapper extends JpaRepository<TicketOrder, Long> {

    Optional<TicketOrder> findByOrderNumber(String orderNumber);

    /** Served by the idx_ticket_order_user index; the Pageable caps the result size. */
    List<TicketOrder> findByUserIdAndTripIdIsNotNullOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
