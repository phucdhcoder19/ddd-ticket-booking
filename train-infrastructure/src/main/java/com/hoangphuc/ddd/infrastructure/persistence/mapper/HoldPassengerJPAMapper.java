package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface HoldPassengerJPAMapper extends JpaRepository<HoldPassenger, Long> {

    List<HoldPassenger> findByHoldIdOrderById(Long holdId);

    /** Served by the idx_hold_passenger_order index. */
    List<HoldPassenger> findByOrderIdInOrderById(Collection<Long> orderIds);

    /**
     * Delete with one DELETE statement, not deleteAll(findBy...).
     *
     * Not only for speed: the JPQL statement hits the DB IMMEDIATELY, while
     * Hibernate's remove() waits until flush — and Hibernate flushes INSERTs
     * before DELETEs. The new row for seat C3-12 would then collide on the
     * unique (holdId, seatCode) with the old row that has not been deleted yet.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("DELETE FROM HoldPassenger p WHERE p.holdId = :holdId")
    int deleteByHold(@Param("holdId") Long holdId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE HoldPassenger p SET p.orderId = :orderId " +
           "WHERE p.holdId = :holdId AND p.orderId IS NULL")
    int attachToOrder(@Param("holdId") Long holdId, @Param("orderId") Long orderId);

    /**
     * "refundedAt IS NULL" is the referee: two refund requests for the same
     * ticket (double click, two devices) both run this, MySQL runs them one
     * after the other, and only the first changes a row.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE HoldPassenger p SET p.refundedAt = :at, p.refundFee = :fee, p.refundAmount = :amount " +
           "WHERE p.id = :id AND p.orderId IS NOT NULL AND p.refundedAt IS NULL")
    int markRefunded(@Param("id") Long id, @Param("fee") long fee,
                     @Param("amount") long amount, @Param("at") LocalDateTime at);
}
