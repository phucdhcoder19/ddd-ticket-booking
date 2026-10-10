package com.hoangphuc.ddd.infrastructure.persistence.repository;

import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;
import com.hoangphuc.ddd.domain.repository.HoldPassengerRepository;
import com.hoangphuc.ddd.infrastructure.persistence.mapper.HoldPassengerJPAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HoldPassengerRepositoryImpl implements HoldPassengerRepository {

    private final HoldPassengerJPAMapper holdPassengerJPAMapper;

    @Override
    @Transactional
    public List<HoldPassenger> replaceForHold(Long holdId, List<HoldPassenger> passengers) {
        holdPassengerJPAMapper.deleteByHold(holdId);
        return holdPassengerJPAMapper.saveAll(passengers);
    }

    @Override
    public List<HoldPassenger> findByHold(Long holdId) {
        return holdPassengerJPAMapper.findByHoldIdOrderById(holdId);
    }

    @Override
    @Transactional
    public int attachToOrder(Long holdId, Long orderId) {
        return holdPassengerJPAMapper.attachToOrder(holdId, orderId);
    }

    @Override
    public List<HoldPassenger> findByOrders(Collection<Long> orderIds) {
        if (orderIds.isEmpty()) {
            return List.of();   // "IN ()" is invalid SQL
        }
        return holdPassengerJPAMapper.findByOrderIdInOrderById(orderIds);
    }

    @Override
    public Optional<HoldPassenger> findById(Long id) {
        return holdPassengerJPAMapper.findById(id);
    }

    @Override
    public int markRefunded(Long id, long fee, long amount, LocalDateTime at) {
        // No @Transactional: joins the transaction of RefundTransactionService
        return holdPassengerJPAMapper.markRefunded(id, fee, amount, at);
    }
}
