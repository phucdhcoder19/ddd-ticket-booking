package com.hoangphuc.ddd.application.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RefundResult {

    public enum Status {
        SUCCESS,
        /** No such ticket, or it belongs to someone else. */
        TICKET_NOT_FOUND,
        ALREADY_REFUNDED,
        /** Inside the cut-off before departure, or the train has already left. */
        TOO_LATE,
        ERROR
    }

    private final Status status;
    private final PassengerTicketDTO ticket;

    public static RefundResult success(PassengerTicketDTO ticket) {
        return new RefundResult(Status.SUCCESS, ticket);
    }

    public static RefundResult fail(Status status) {
        return new RefundResult(status, null);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
