package com.hoangphuc.ddd.application.service.trip.impl;

import com.hoangphuc.ddd.application.model.CarriageDTO;
import com.hoangphuc.ddd.application.model.SeatDTO;
import com.hoangphuc.ddd.application.service.pricing.Journey;
import com.hoangphuc.ddd.application.service.pricing.JourneyPricingService;
import com.hoangphuc.ddd.application.service.trip.SeatMapAppService;
import com.hoangphuc.ddd.domain.model.entity.Seat;
import com.hoangphuc.ddd.domain.model.entity.TrainCarriage;
import com.hoangphuc.ddd.domain.model.entity.Trip;
import com.hoangphuc.ddd.domain.repository.SeatRepository;
import com.hoangphuc.ddd.domain.repository.TrainRepository;
import com.hoangphuc.ddd.domain.repository.TripRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class SeatMapAppServiceImpl implements SeatMapAppService {

    private final TripRepository tripRepository;
    private final TrainRepository trainRepository;
    private final SeatRepository seatRepository;
    private final JourneyPricingService journeyPricingService;

    /**
     * TWO round trips to the DB for the whole seat map, however many carriages:
     *
     *   1 query for the train's carriage templates
     *   1 query for ALL seats of that seat class, then grouped by carriage in memory
     *
     * No loop of "for each carriage, load its seats": a sleeper class has 5
     * carriages, one query each is 5 round trips; people click back and forth
     * between seat classes on this screen, so the count multiplies quickly.
     * Grouping in memory is much cheaper because the rows returned are the same.
     */
    @Override
    public Optional<List<CarriageDTO>> carriages(Long tripId, String seatClass,
                                                 String fromCode, String toCode) {
        Optional<Trip> found = tripRepository.findById(tripId);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Trip trip = found.get();

        Optional<Journey> journey = journeyPricingService.resolve(fromCode, toCode, trip.getServiceDate());
        if (journey.isEmpty()) {
            return Optional.empty();
        }

        Map<Integer, List<Seat>> seatsByCarriage = new LinkedHashMap<>();
        for (Seat seat : seatRepository.findByTripAndClass(tripId, seatClass)) {
            seatsByCarriage.computeIfAbsent(seat.getCarriageNumber(), k -> new ArrayList<>()).add(seat);
        }

        List<CarriageDTO> result = new ArrayList<>();
        for (TrainCarriage carriage : trainRepository.findCarriages(trip.getTrainId())) {
            if (!carriage.getSeatClass().equals(seatClass)) {
                continue;
            }
            List<Seat> seats = seatsByCarriage.getOrDefault(carriage.getNumber(), List.of());
            result.add(toDTO(trip, carriage, seats, journey.get()));
        }
        result.sort(Comparator.comparingInt(CarriageDTO::getNumber));

        log.info("[SEAT-MAP] trip={} class={} | {} carriages", tripId, seatClass, result.size());
        return Optional.of(result);
    }

    private CarriageDTO toDTO(Trip trip, TrainCarriage carriage, List<Seat> seats, Journey journey) {
        List<SeatDTO> seatDTOs = new ArrayList<>(seats.size());
        int available = 0;
        for (Seat seat : seats) {
            if (seat.isFree()) {
                available++;
            }
            seatDTOs.add(toDTO(seat, journey));
        }

        CarriageDTO dto = new CarriageDTO();
        // Unique id within a trip — the frontend uses it as the React key.
        dto.setId(trip.getId() + "-C" + carriage.getNumber());
        dto.setNumber(carriage.getNumber());
        dto.setSeatClass(carriage.getSeatClass());
        dto.setLayout(carriage.getLayout());
        dto.setRows(carriage.getRowCount());
        dto.setSeats(seatDTOs);
        dto.setAvailable(available);
        return dto;
    }

    private SeatDTO toDTO(Seat seat, Journey journey) {
        SeatDTO dto = new SeatDTO();
        dto.setId(seat.getSeatCode());
        dto.setLabel(seat.getLabel());
        dto.setRow(seat.getRowNo());
        dto.setCol(seat.getColNo());
        dto.setCompartment(seat.getCompartment() > 0 ? seat.getCompartment() : null);
        dto.setBerthLevel(seat.getBerthLevel() > 0 ? seat.getBerthLevel() : null);
        dto.setStatus(seat.statusName());
        dto.setPrice(journeyPricingService.fareOf(journey, seat));
        return dto;
    }
}
