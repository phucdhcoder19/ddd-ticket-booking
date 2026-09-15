package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.TicketOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TicketOrderJPAMapper extends JpaRepository<TicketOrder, Long> {

    Optional<TicketOrder> findByOrderNumber(String orderNumber);
}
