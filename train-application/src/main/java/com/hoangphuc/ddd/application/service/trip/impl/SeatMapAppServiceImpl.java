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
     * HAI luot xuong DB cho ca so do, du toa nao cung phai ve:
     *
     *   1 cau lay khuon mau toa cua doan tau
     *   1 cau lay TOAN BO ghe cua hang cho do, roi gom theo toa trong RAM
     *
     * Khong lap "voi moi toa, lay ghe cua toa do": mot toa nam co 5 toa, moi
     * toa mot cau la 5 luot; man hinh nay nguoi ta bam qua lai giua cac hang
     * cho lien tuc nen so luot nhan len rat nhanh. Gom trong RAM re hon nhieu
     * vi du lieu tra ve van la tung ay dong.
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

        log.info("[SO-DO] chuyen={} hang={} | {} toa", tripId, seatClass, result.size());
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
        // Id duy nhat trong pham vi mot chuyen — frontend dung lam key React.
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
