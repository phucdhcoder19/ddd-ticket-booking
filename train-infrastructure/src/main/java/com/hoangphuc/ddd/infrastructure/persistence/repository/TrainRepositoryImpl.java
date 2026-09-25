package com.hoangphuc.ddd.infrastructure.persistence.repository;

import com.hoangphuc.ddd.domain.model.entity.Train;
import com.hoangphuc.ddd.domain.model.entity.TrainCarriage;
import com.hoangphuc.ddd.domain.repository.TrainRepository;
import com.hoangphuc.ddd.infrastructure.persistence.mapper.TrainCarriageJPAMapper;
import com.hoangphuc.ddd.infrastructure.persistence.mapper.TrainJPAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TrainRepositoryImpl implements TrainRepository {

    private static final int STATUS_ACTIVE = 1;

    private final TrainJPAMapper trainJPAMapper;
    private final TrainCarriageJPAMapper trainCarriageJPAMapper;

    @Override
    public List<Train> findActive() {
        return trainJPAMapper.findByStatusOrderByDepartHourAscDepartMinuteAsc(STATUS_ACTIVE);
    }

    @Override
    public Optional<Train> findById(Long trainId) {
        return trainJPAMapper.findById(trainId);
    }

    @Override
    public List<TrainCarriage> findCarriages(Long trainId) {
        return trainCarriageJPAMapper.findByTrainIdOrderByNumberAsc(trainId);
    }
}
