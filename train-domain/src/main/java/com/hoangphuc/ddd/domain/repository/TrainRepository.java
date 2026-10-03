package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.Train;
import com.hoangphuc.ddd.domain.model.entity.TrainCarriage;

import java.util.List;
import java.util.Optional;

public interface TrainRepository {

    /** Trains in service, sorted by departure time. */
    List<Train> findActive();

    Optional<Train> findById(Long trainId);

    /** Carriage layout of a train — a template, not tied to a date. */
    List<TrainCarriage> findCarriages(Long trainId);
}
