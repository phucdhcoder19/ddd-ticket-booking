package com.hoangphuc.ddd.infrastructure.persistence.repository;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.repository.TicketDetailRepository;
import com.hoangphuc.ddd.infrastructure.persistence.mapper.TicketDetailJPAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TicketDetailRepositoryImpl implements TicketDetailRepository {

    private final TicketDetailJPAMapper ticketDetailJPAMapper;

    @Override
    public TicketDetail findById(Long ticketId) {
        return ticketDetailJPAMapper.findById(ticketId).orElse(null);
    }

    @Override
    public int getStockAvailable(Long ticketId) {
        Integer stock = ticketDetailJPAMapper.getStockAvailable(ticketId);
        return stock == null ? -1 : stock;      // -1 = không tìm thấy vé
    }

    @Override
    public Optional<TicketDetail> findNextOpening(LocalDateTime now) {
        return ticketDetailJPAMapper
                .findFirstByStatusAndSaleStartTimeAfterOrderBySaleStartTimeAsc(TicketDetail.STATUS_ACTIVE, now);
    }

    @Override
    public Optional<TicketDetail> findLatestOpened(LocalDateTime now) {
        return ticketDetailJPAMapper
                .findFirstByStatusAndSaleStartTimeLessThanEqualOrderBySaleStartTimeDesc(TicketDetail.STATUS_ACTIVE, now);
    }

    @Override
    @Transactional
    public boolean decreaseStock(Long ticketId, int quantity) {
        return ticketDetailJPAMapper.decreaseStock(ticketId, quantity) > 0;
    }

    @Override
    @Transactional
    public boolean increaseStock(Long ticketId, int quantity) {
        return ticketDetailJPAMapper.increaseStock(ticketId, quantity) > 0;
    }
}
