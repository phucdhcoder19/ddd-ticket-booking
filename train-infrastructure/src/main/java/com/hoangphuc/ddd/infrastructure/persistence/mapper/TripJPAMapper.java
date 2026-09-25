package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripJPAMapper extends JpaRepository<Trip, Long> {
    Optional<Trip> findByTrainIdAndServiceDate(Long trainId, LocalDate serviceDate);
    List<Trip> findByServiceDate(LocalDate serviceDate);
}
