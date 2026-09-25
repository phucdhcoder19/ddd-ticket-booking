package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.Train;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TrainJPAMapper extends JpaRepository<Train, Long> {
    List<Train> findByStatusOrderByDepartHourAscDepartMinuteAsc(int status);
}
