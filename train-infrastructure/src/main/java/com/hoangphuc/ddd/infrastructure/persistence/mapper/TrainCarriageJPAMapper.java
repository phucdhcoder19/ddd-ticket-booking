package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.TrainCarriage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainCarriageJPAMapper extends JpaRepository<TrainCarriage, Long> {
    List<TrainCarriage> findByTrainIdOrderByNumberAsc(Long trainId);
}
