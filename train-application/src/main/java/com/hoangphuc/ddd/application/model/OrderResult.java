package com.hoangphuc.ddd.application.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderResult {

    public enum Status {
        SUCCESS,
        HOLD_NOT_FOUND,
        /** Looked up an order number that does not exist. */
        ORDER_NOT_FOUND,
        /** The hold expired, was released by the job, or has already become an order. */
        HOLD_EXPIRED,
        /** Not every seat has a passenger yet — the hold is still alive, the customer can go back and fill it in. */
        PASSENGERS_MISSING,
        TICKET_NOT_FOUND,
        ERROR
    }

    private final Status status;
    private final OrderDTO order;

    public static OrderResult success(OrderDTO order) {
        return new OrderResult(Status.SUCCESS, order);
    }

    public static OrderResult fail(Status status) {
        return new OrderResult(status, null);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
