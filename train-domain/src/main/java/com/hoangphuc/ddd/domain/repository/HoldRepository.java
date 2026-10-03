package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.Hold;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HoldRepository {

    Hold save(Hold hold);

    Optional<Hold> findByCode(String holdCode);

    /**
     * Claim the right to close a hold.
     *
     * Returns the number of affected rows, NOT a boolean "success":
     *   1 = we changed the status -> we are allowed to return the seats
     *   0 = someone else got there first -> back off, do not touch the seats
     *
     * This blocks both traps: the job running twice on 2 servers, and the job
     * stealing the hold of a customer who is in the middle of paying.
     */
    int markReleased(Long holdId, LocalDateTime now);

    int markUsed(Long holdId, LocalDateTime now);

    /**
     * Lock the hold row until the current transaction ends, if it is still active.
     * 0 = expired / used / cancelled -> nothing may be changed anymore.
     */
    int lockIfHolding(Long holdId, LocalDateTime now);

    /** Holds that have expired but have not been cleaned up yet. */
    List<Hold> findExpired(LocalDateTime now, int limit);
}
