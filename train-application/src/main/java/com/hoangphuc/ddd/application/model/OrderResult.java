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
        /** Hold đã hết hạn, đã bị job thu hồi, hoặc đã đổi thành đơn rồi. */
        HOLD_EXPIRED,
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
