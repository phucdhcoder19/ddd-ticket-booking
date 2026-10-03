package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.Hold;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HoldJPAMapper extends JpaRepository<Hold, Long> {

    Optional<Hold> findByHoldCode(String holdCode);

    /**
     * CLOSING A HOLD — the two queries below are the whole of lesson 18.
     *
     * The "AND h.status = 0" clause is the lock. InnoDB locks the row on
     * UPDATE, so two parties touching the same row must queue; whoever comes
     * second reads the LATEST value, sees status is no longer 0, the condition
     * fails, 0 rows updated.
     *
     * No distributed lock, no Redisson. One WHERE clause does it all.
     *
     * The TIMESTAMP is passed in by the APP; CURRENT_TIMESTAMP is not used.
     * CURRENT_TIMESTAMP is MySQL's clock — the container runs in UTC while the
     * app runs in +07, so the same row would have expire_at and updated_at 7
     * hours apart. The whole system must read time from ONE clock.
     */
    @Modifying
    @Query("UPDATE Hold h SET h.status = 2, h.updatedAt = :now " +
           "WHERE h.id = :holdId AND h.status = 0")
    int markReleased(@Param("holdId") Long holdId, @Param("now") LocalDateTime now);

    /**
     * Turn a hold into an order.
     *
     * It has an EXTRA condition "AND h.expireAt > :now" that markReleased does
     * not need. Reason: the job scans every 10 seconds, so there is a gap
     * between the moment a hold expires and the moment the job cleans it up —
     * during that gap status is still 0. Checking status alone would let a
     * customer use an expired hold.
     *
     * The time check must be INSIDE the UPDATE, not an if in the layer above:
     * read-then-write leaves a gap, while putting it in the WHERE makes MySQL
     * lock the row and check against the latest value.
     */
    @Modifying
    @Query("UPDATE Hold h SET h.status = 1, h.updatedAt = :now " +
           "WHERE h.id = :holdId AND h.status = 0 AND h.expireAt > :now")
    int markUsed(@Param("holdId") Long holdId, @Param("now") LocalDateTime now);

    /**
     * Lock the hold row until the transaction ends, and confirm it is still valid.
     *
     * Changes no state, only touches updatedAt — the point is the ROW LOCK
     * InnoDB takes on UPDATE. markUsed() also UPDATEs this same row, so
     * "save passengers" and "create order" running at the same time must
     * queue: the order never reads a half-written passenger list.
     */
    @Modifying
    @Query("UPDATE Hold h SET h.updatedAt = :now " +
           "WHERE h.id = :holdId AND h.status = 0 AND h.expireAt > :now")
    int lockIfHolding(@Param("holdId") Long holdId, @Param("now") LocalDateTime now);

    /**
     * Scan in batches, never everything at once. At peak time thousands of
     * holds can expire together; loading them all into memory makes one long
     * transaction that locks many rows and blocks people who are buying.
     */
    @Query("SELECT h FROM Hold h WHERE h.status = 0 AND h.expireAt < :now ORDER BY h.expireAt ASC")
    List<Hold> findExpired(@Param("now") LocalDateTime now, Pageable pageable);
}
