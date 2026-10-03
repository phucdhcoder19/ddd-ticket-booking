package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.HoldPassenger;

import java.util.List;

public interface HoldPassengerRepository {

    /**
     * Replace the WHOLE passenger list of a hold.
     *
     * Delete everything and write again instead of updating row by row: PUT
     * means "this is the final state", so calling it twice gives the same
     * result. The frontend retries automatically on slow networks, so that
     * property is required, not just nice to have.
     */
    List<HoldPassenger> replaceForHold(Long holdId, List<HoldPassenger> passengers);

    List<HoldPassenger> findByHold(Long holdId);

    /** Attach the passengers of a hold to the order that was just created. */
    int attachToOrder(Long holdId, Long orderId);
}
