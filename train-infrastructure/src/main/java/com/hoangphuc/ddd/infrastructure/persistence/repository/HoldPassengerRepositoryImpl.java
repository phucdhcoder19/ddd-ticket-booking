package com.hoangphuc.ddd.infrastructure.persistence.repository;

import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;
import com.hoangphuc.ddd.domain.repository.HoldPassengerRepository;
import com.hoangphuc.ddd.infrastructure.persistence.mapper.HoldPassengerJPAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
}
