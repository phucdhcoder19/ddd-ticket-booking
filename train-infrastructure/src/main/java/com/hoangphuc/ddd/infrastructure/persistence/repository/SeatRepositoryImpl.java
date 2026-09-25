package com.hoangphuc.ddd.infrastructure.persistence.repository;

import com.hoangphuc.ddd.domain.model.entity.Seat;
import com.hoangphuc.ddd.domain.repository.SeatRepository;
import com.hoangphuc.ddd.infrastructure.persistence.mapper.SeatJPAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class SeatRepositoryImpl implements SeatRepository {

    private final SeatJPAMapper seatJPAMapper;

    @Override
    public List<Seat> saveAll(List<Seat> seats) {
        return seatJPAMapper.saveAll(seats);
    }

    @Override
    public long countByTrip(Long tripId) {
        return seatJPAMapper.countByTripId(tripId);
    }

    @Override
    public List<Object[]> countFreeByClass(Long tripId) {
        return seatJPAMapper.countFreeByClass(tripId);
    }

    @Override
    public List<Seat> findByTripAndCarriage(Long tripId, int carriageNumber) {
        return seatJPAMapper.findByTripIdAndCarriageNumberOrderByRowNoAscColNoAsc(tripId, carriageNumber);
    }

    @Override
    public List<Seat> findByTripAndClass(Long tripId, String seatClass) {
        return seatJPAMapper.findByTripIdAndSeatClassOrderByCarriageNumberAscRowNoAscColNoAsc(tripId, seatClass);
    }

    @Override
    public List<Seat> findByTripAndCodes(Long tripId, Collection<String> seatCodes) {
        return seatJPAMapper.findByTripIdAndSeatCodeIn(tripId, seatCodes);
    }

    @Override
    public List<Seat> findByHold(Long holdId) {
        return seatJPAMapper.findByHoldIdOrderByCarriageNumberAscRowNoAscColNoAsc(holdId);
    }

    @Override
    public List<Seat> findByOrder(Long orderId) {
        return seatJPAMapper.findByOrderIdOrderByCarriageNumberAscRowNoAscColNoAsc(orderId);
    }

    @Override
    public int claimForHold(Long tripId, Collection<String> seatCodes, Long holdId) {
        return seatJPAMapper.claimForHold(tripId, seatCodes, holdId);
    }

    @Override
    public int releaseByHold(Long holdId) {
        return seatJPAMapper.releaseByHold(holdId);
    }

    @Override
    public int sellByHold(Long holdId, Long orderId) {
        return seatJPAMapper.sellByHold(holdId, orderId);
    }
}
