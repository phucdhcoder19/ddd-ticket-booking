package com.hoangphuc.ddd.domain.service.impl;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.repository.TicketDetailRepository;
import com.hoangphuc.ddd.domain.service.TicketDetailDomainService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TicketDetailDomainServiceImpl implements TicketDetailDomainService {

    private final TicketDetailRepository ticketDetailRepository;

    @Override
    public TicketDetail getTicketDetailById(Long ticketId) {
        return ticketDetailRepository.findById(ticketId);
    }

    @Override
    public int getStockAvailable(Long ticketId) {
        return ticketDetailRepository.getStockAvailable(ticketId);
    }

    @Override
    public boolean decreaseStock(Long ticketId, int quantity) {
        if (quantity <= 0) return false;        // đặt validate ở đây, đừng để rơi xuống SQL
        return ticketDetailRepository.decreaseStock(ticketId, quantity);
    }

    @Override
    public boolean increaseStock(Long ticketId, int quantity) {
        return ticketDetailRepository.increaseStock(ticketId, quantity);
    }
}
