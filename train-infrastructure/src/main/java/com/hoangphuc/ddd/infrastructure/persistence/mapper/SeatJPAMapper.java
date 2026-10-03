package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.Seat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface SeatJPAMapper extends JpaRepository<Seat, Long> {

    long countByTripId(Long tripId);

    List<Seat> findByTripIdAndCarriageNumberOrderByRowNoAscColNoAsc(Long tripId, int carriageNumber);

    List<Seat> findByTripIdAndSeatClassOrderByCarriageNumberAscRowNoAscColNoAsc(Long tripId, String seatClass);

    List<Seat> findByTripIdAndSeatCodeIn(Long tripId, Collection<String> seatCodes);

    List<Seat> findByHoldIdOrderByCarriageNumberAscRowNoAscColNoAsc(Long holdId);

    List<Seat> findByOrderIdOrderByCarriageNumberAscRowNoAscColNoAsc(Long orderId);

    /**
     * Count free seats, grouped by seat class.
     *
     * One GROUP BY query instead of N separate counts. The trip search screen
     * shows 7 trains x 3 seat classes = 21 numbers; counting each one would be
     * 21 round trips to the DB.
     *
     * Returns Object[]{ seatClass, freeSeats } — this is the infrastructure
     * boundary, the layer above converts it into a meaningful type.
     */
    @Query("SELECT s.seatClass, COUNT(s) FROM Seat s " +
           "WHERE s.tripId = :tripId AND s.status = 0 GROUP BY s.seatClass")
    List<Object[]> countFreeByClass(@Param("tripId") Long tripId);

    /**
     * CLAIM SEATS — this is the entire contention handling of the seat picker.
     *
     * The "AND s.status = 0" clause is the lock, exactly like Hold's
     * markReleased(): InnoDB locks the row on UPDATE, so two people clicking
     * seat C3-12 at once must queue; the second one reads the LATEST value,
     * sees status is already 1, the condition fails, 0 rows updated.
     *
     * No Redisson needed here. A distributed lock is only needed when the thing
     * to protect lives OUTSIDE the database (like provisioning a trip, writing
     * 600 rows); when people fight over exactly one row, the database itself
     * is already the referee.
     *
     * The returned row count is the number of seats claimed. The layer above
     * compares it with the number requested — missing even one seat must
     * ROLLBACK the whole hold, because the passenger picked 3 adjacent seats,
     * not "any 3 free seats".
     *
     * clearAutomatically: right after this UPDATE the layer above reads the
     * same rows back. Without clearing the persistence context, Hibernate
     * returns the copy still in session memory — old status, empty holdId.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Seat s SET s.status = 1, s.holdId = :holdId " +
           "WHERE s.tripId = :tripId AND s.seatCode IN :seatCodes AND s.status = 0")
    int claimForHold(@Param("tripId") Long tripId,
                     @Param("seatCodes") Collection<String> seatCodes,
                     @Param("holdId") Long holdId);

    /**
     * RETURN SEATS. "AND s.status = 1" so a SOLD seat is never pulled back to
     * free: a release job running right after the customer paid finds 0 rows
     * and moves on, instead of reselling a seat that already has an owner.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Seat s SET s.status = 0, s.holdId = null " +
           "WHERE s.holdId = :holdId AND s.status = 1")
    int releaseByHold(@Param("holdId") Long holdId);

    /** Seats become sold. holdId is kept so the hold can be traced back. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Seat s SET s.status = 2, s.orderId = :orderId " +
           "WHERE s.holdId = :holdId AND s.status = 1")
    int sellByHold(@Param("holdId") Long holdId, @Param("orderId") Long orderId);
}
