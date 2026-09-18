package com.hoangphuc.ddd.infrastructure.persistence.repository;

import com.hoangphuc.ddd.domain.model.entity.Hold;
import com.hoangphuc.ddd.domain.repository.HoldRepository;
import com.hoangphuc.ddd.infrastructure.persistence.mapper.HoldJPAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class HoldRepositoryImpl implements HoldRepository {

    private final HoldJPAMapper holdJPAMapper;

    @Override
    public Hold save(Hold hold) {
        return holdJPAMapper.save(hold);
    }

    @Override
    public Optional<Hold> findByCode(String holdCode) {
        return holdJPAMapper.findByHoldCode(holdCode);
    }

    @Override
    @Transactional
    public int markReleased(Long holdId, LocalDateTime now) {
        return holdJPAMapper.markReleased(holdId, now);
    }

    @Override
    @Transactional
    public int markUsed(Long holdId, LocalDateTime now) {
        return holdJPAMapper.markUsed(holdId, now);
    }

    @Override
    public List<Hold> findExpired(LocalDateTime now, int limit) {
        return holdJPAMapper.findExpired(now, PageRequest.of(0, limit));
    }
}
