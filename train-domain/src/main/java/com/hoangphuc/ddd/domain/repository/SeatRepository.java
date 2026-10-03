package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.Seat;

import java.util.Collection;
import java.util.List;

public interface SeatRepository {

    /** Save a whole batch of seats when a new trip is provisioned. */
    List<Seat> saveAll(List<Seat> seats);

    long countByTrip(Long tripId);

    /** Count free seats per seat class — feeds the trip search screen. */
    List<Object[]> countFreeByClass(Long tripId);

    /** Seat map of one carriage. */
    List<Seat> findByTripAndCarriage(Long tripId, int carriageNumber);

    List<Seat> findByTripAndClass(Long tripId, String seatClass);

    List<Seat> findByTripAndCodes(Long tripId, Collection<String> seatCodes);

    /** Seats currently occupied by a hold. */
    List<Seat> findByHold(Long holdId);

    /** Seats bought by an order. */
    List<Seat> findByOrder(Long orderId);

    /**
     * CLAIM SEATS for a hold.
     *
     * Returns the NUMBER OF SEATS claimed. The caller compares it with the
     * number requested: missing even one seat is a failure, because the
     * passenger picked 3 seats next to each other, not "any 3 seats".
     */
    int claimForHold(Long tripId, Collection<String> seatCodes, Long holdId);

    /** Return seats to stock when a hold is cancelled or expires. */
    int releaseByHold(Long holdId);

    /** Move seats from "held" to "sold" and attach them to the order. */
    int sellByHold(Long holdId, Long orderId);
}
