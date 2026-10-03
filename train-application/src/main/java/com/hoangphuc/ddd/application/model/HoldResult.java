package com.hoangphuc.ddd.application.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Result of a hold operation. No HTTP codes here — mapping Status to
 * 200/409/410 is the controller's job.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HoldResult {

    public enum Status {
        SUCCESS,
        /** Not admitted by the waiting room, or the admission has expired. */
        QUEUE_REQUIRED,
        /** No such trip, or its seats have not been generated yet. */
        TRIP_NOT_FOUND,
        /** Unknown station code, or departure equals arrival. */
        INVALID_ROUTE,
        /** Someone else took at least one of the selected seats. */
        SEAT_TAKEN,
        NOT_ON_SALE,
        SALE_ENDED,
        HOLD_NOT_FOUND,
        HOLD_EXPIRED,     // past its deadline, or already closed
        /** The passenger list does not match the hold's seats one to one. */
        PASSENGER_MISMATCH,
        ERROR
    }

    private final Status status;
    private final HoldDTO hold;

    public static HoldResult success(HoldDTO hold) {
        return new HoldResult(Status.SUCCESS, hold);
    }

    public static HoldResult fail(Status status) {
        return new HoldResult(status, null);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
