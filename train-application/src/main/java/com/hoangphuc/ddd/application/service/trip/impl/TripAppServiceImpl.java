package com.hoangphuc.ddd.application.service.trip.impl;

import com.hoangphuc.ddd.application.model.SeatClassOfferDTO;
import com.hoangphuc.ddd.application.model.StationDTO;
import com.hoangphuc.ddd.application.model.TripDTO;
import com.hoangphuc.ddd.application.service.pricing.Journey;
import com.hoangphuc.ddd.application.service.pricing.JourneyPricingService;
import com.hoangphuc.ddd.application.service.trip.TripAppService;
import com.hoangphuc.ddd.application.service.trip.TripProvisionService;
import com.hoangphuc.ddd.domain.model.entity.Station;
import com.hoangphuc.ddd.domain.model.entity.Train;
import com.hoangphuc.ddd.domain.model.entity.TrainCarriage;
import com.hoangphuc.ddd.domain.model.entity.Trip;
import com.hoangphuc.ddd.domain.repository.SeatRepository;
import com.hoangphuc.ddd.domain.repository.TrainRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class TripAppServiceImpl implements TripAppService {

    /** Time spent stopped at stations along the way, added to the pure running time. */
    private static final int DWELL_MINUTES = 25;

    /** Cheapest berth level of each class — used for the "from ... VND" price. */
    private static final Map<String, Integer> CHEAPEST_BERTH = Map.of(
            TrainCarriage.CLASS_SOFT_SEAT, 0,
            TrainCarriage.CLASS_BERTH_4, 2,
            TrainCarriage.CLASS_BERTH_6, 3
    );

    private final TrainRepository trainRepository;
    private final SeatRepository seatRepository;
    private final JourneyPricingService journeyPricingService;
    private final TripProvisionService tripProvisionService;

    @Override
    public List<TripDTO> search(String fromCode, String toCode, LocalDate date) {
        Optional<Journey> journey = journeyPricingService.resolve(fromCode, toCode, date);
        if (journey.isEmpty()) {
            return List.of();
        }

        List<TripDTO> result = new ArrayList<>();
        for (Train train : trainRepository.findActive()) {
            // Provision the trip if nobody has searched this date yet. Costs ~1s
            // for the first person; later searches read straight from the DB.
            Optional<Trip> trip = tripProvisionService.ensureTrip(train, date);
            if (trip.isEmpty()) {
                continue;
            }
            result.add(toDTO(train, trip.get(), journey.get()));
        }
        return result;
    }

    /**
     * Count free seats per class, and compute the lowest price of each class.
     *
     * One GROUP BY for the whole trip instead of counting each class — the
     * search screen shows 7 trains × 3 classes, counting one by one would be
     * 21 round trips to the DB.
     */
    private TripDTO toDTO(Train train, Trip trip, Journey journey) {
        Map<String, Integer> freeByClass = new HashMap<>();
        for (Object[] row : seatRepository.countFreeByClass(trip.getId())) {
            freeByClass.put((String) row[0], ((Number) row[1]).intValue());
        }

        Map<String, Integer> totalByClass = new HashMap<>();
        for (TrainCarriage carriage : trainRepository.findCarriages(train.getId())) {
            totalByClass.merge(carriage.getSeatClass(), carriage.seatCount(), Integer::sum);
        }

        List<SeatClassOfferDTO> classes = new ArrayList<>();
        int availableTotal = 0;
        for (Map.Entry<String, Integer> e : totalByClass.entrySet()) {
            String seatClass = e.getKey();
            int free = freeByClass.getOrDefault(seatClass, 0);
            availableTotal += free;
            long price = journeyPricingService.fareOf(
                    journey, seatClass, CHEAPEST_BERTH.getOrDefault(seatClass, 0));
            classes.add(new SeatClassOfferDTO(seatClass, price, free, e.getValue()));
        }
        classes.sort((a, b) -> Long.compare(a.getPrice(), b.getPrice()));

        LocalDateTime departAt = trip.getServiceDate()
                .atTime(train.getDepartHour(), train.getDepartMinute());
        int durationMinutes =
                (int) Math.round(journey.distanceKm() * 60d / train.getSpeedKmh()) + DWELL_MINUTES;

        TripDTO dto = new TripDTO();
        dto.setId(String.valueOf(trip.getId()));
        dto.setTrainCode(train.getCode());
        dto.setFromStation(toStationDTO(journey.from()));
        dto.setToStation(toStationDTO(journey.to()));
        dto.setDepartAt(departAt);
        dto.setArriveAt(departAt.plusMinutes(durationMinutes));
        dto.setDurationMinutes(durationMinutes);
        dto.setClasses(classes);
        dto.setAvailableTotal(availableTotal);
        return dto;
    }

    private StationDTO toStationDTO(Station s) {
        StationDTO dto = new StationDTO();
        dto.setCode(s.getCode());
        dto.setName(s.getName());
        dto.setRegion(s.getRegion());
        return dto;
    }
}
