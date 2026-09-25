package com.hoangphuc.ddd.infrastructure.persistence.repository;

import com.hoangphuc.ddd.domain.model.entity.Trip;
import com.hoangphuc.ddd.domain.repository.TripRepository;
import com.hoangphuc.ddd.infrastructure.persistence.mapper.TripJPAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class TripRepositoryImpl implements TripRepository {

    private final TripJPAMapper tripJPAMapper;

    @Override
    public Optional<Trip> findByTrainAndDate(Long trainId, LocalDate serviceDate) {
        return tripJPAMapper.findByTrainIdAndServiceDate(trainId, serviceDate);
    }

    @Override
    public List<Trip> findByDate(LocalDate serviceDate) {
        return tripJPAMapper.findByServiceDate(serviceDate);
    }

    @Override
    public Optional<Trip> findById(Long tripId) {
        return tripJPAMapper.findById(tripId);
    }

    @Override
    public Trip save(Trip trip) {
        return tripJPAMapper.save(trip);
    }
}
