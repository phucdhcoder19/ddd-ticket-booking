package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.Trip;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TripRepository {

    Optional<Trip> findByTrainAndDate(Long trainId, LocalDate serviceDate);

    List<Trip> findByDate(LocalDate serviceDate);

    Optional<Trip> findById(Long tripId);

    Trip save(Trip trip);
}
