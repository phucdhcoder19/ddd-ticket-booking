package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface HoldPassengerJPAMapper extends JpaRepository<HoldPassenger, Long> {

    List<HoldPassenger> findByHoldIdOrderById(Long holdId);

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
}
