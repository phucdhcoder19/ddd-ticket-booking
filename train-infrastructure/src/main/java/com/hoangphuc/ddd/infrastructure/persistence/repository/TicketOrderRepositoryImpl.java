package com.hoangphuc.ddd.infrastructure.persistence.repository;

import com.hoangphuc.ddd.domain.model.entity.TicketOrder;
import com.hoangphuc.ddd.domain.repository.TicketOrderRepository;
import com.hoangphuc.ddd.infrastructure.persistence.mapper.TicketOrderJPAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class TicketOrderRepositoryImpl implements TicketOrderRepository {

    private final TicketOrderJPAMapper ticketOrderJPAMapper;

    @Override
    public TicketOrder save(TicketOrder order) {
        // NO @Transactional here: Spring Data's save() joins the transaction
        // already opened by the layer above (OrderTransactionService).
        return ticketOrderJPAMapper.save(order);
    }

    @Override
    public TicketOrder findByOrderNumber(String orderNumber) {
        return ticketOrderJPAMapper.findByOrderNumber(orderNumber).orElse(null);
    }

    @Override
    public TicketOrder findById(Long id) {
        return ticketOrderJPAMapper.findById(id).orElse(null);
    }

    @Override
    public List<TicketOrder> findSeatOrdersByUser(Long userId, int limit) {
        return ticketOrderJPAMapper.findByUserIdAndTripIdIsNotNullOrderByCreatedAtDesc(userId, PageRequest.of(0, limit));
    }
}
