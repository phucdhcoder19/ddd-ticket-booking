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
 * Ghi chuyến + toàn bộ ghế trong MỘT transaction.
 *
 * Tách khỏi TripProvisionService vì đúng hai lý do đã gặp ở
 * OrderTransactionService và HoldTransactionService:
 *
 *   1. @Transactional chỉ ăn khi được gọi TỪ NGOÀI class, qua proxy Spring.
 *      Để chung một class rồi gọi this.createTripWithSeats(...) là mất
 *      transaction, lặng lẽ, không báo lỗi.
 *   2. Ở đây còn một hệ quả nặng hơn: mất transaction thì crash giữa chừng
 *      để lại dòng trip RỖNG không có ghế. Và vì lần sau tìm thấy trip đó,
 *      hệ thống sẽ không bao giờ sinh lại ghế nữa — chuyến chết vĩnh viễn.
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

        log.info("[TRIP] sinh chuyen | tau={} ngay={} toa={} ghe={}",
                train.getCode(), serviceDate, carriages.size(), seats.size());
        return trip;
    }
}
