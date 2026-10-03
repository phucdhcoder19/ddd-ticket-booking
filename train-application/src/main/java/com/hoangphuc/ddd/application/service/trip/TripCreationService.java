package com.hoangphuc.ddd.application.service.trip;

import com.hoangphuc.ddd.domain.model.entity.Seat;
import com.hoangphuc.ddd.domain.model.entity.Train;
import com.hoangphuc.ddd.domain.model.entity.TrainCarriage;
import com.hoangphuc.ddd.domain.model.entity.Trip;
import com.hoangphuc.ddd.domain.repository.SeatRepository;
import com.hoangphuc.ddd.domain.repository.TrainRepository;
import com.hoangphuc.ddd.domain.repository.TripRepository;
import com.hoangphuc.ddd.domain.service.SeatFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Writes the trip + all of its seats in ONE transaction.
 *
 * Kept apart from TripProvisionService for exactly the two reasons seen in
 * OrderTransactionService and HoldTransactionService:
 *
 *   1. @Transactional only works when called FROM OUTSIDE the class, through
 *      the Spring proxy. Putting it in the same class and calling
 *      this.createTripWithSeats(...) silently loses the transaction.
 *   2. Here the consequence is worse: without a transaction, a crash midway
 *      leaves an EMPTY trip row with no seats. And because the next search
 *      finds that trip, the system never generates seats again — the trip is
 *      dead forever.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TripCreationService {

    private final TrainRepository trainRepository;
    private final TripRepository tripRepository;
    private final SeatRepository seatRepository;

    @Transactional(rollbackFor = Exception.class)
    public Trip createTripWithSeats(Train train, LocalDate serviceDate) {
        Trip trip = tripRepository.save(new Trip()
                .setTrainId(train.getId())
                .setServiceDate(serviceDate)
                .setStatus(Trip.STATUS_READY)
                .setCreatedAt(LocalDateTime.now()));

        List<TrainCarriage> carriages = trainRepository.findCarriages(train.getId());
        List<Seat> seats = new ArrayList<>();
        for (TrainCarriage carriage : carriages) {
            seats.addAll(SeatFactory.build(trip.getId(), carriage));
        }
        seatRepository.saveAll(seats);

        log.info("[TRIP] trip provisioned | train={} date={} carriages={} seats={}",
                train.getCode(), serviceDate, carriages.size(), seats.size());
        return trip;
    }
}
