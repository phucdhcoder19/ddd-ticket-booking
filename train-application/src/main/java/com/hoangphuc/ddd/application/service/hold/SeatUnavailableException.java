package com.hoangphuc.ddd.application.service.hold;

/**
 * At least one of the seats the customer picked was taken by someone else.
 *
 * A RuntimeException rather than a return value, because it is thrown FROM
 * INSIDE the transaction: only throwing makes Spring ROLL BACK. Returning null
 * would commit normally and leave a hold with 2 of 3 seats — exactly what this
 * business does not allow.
 */
public class SeatUnavailableException extends RuntimeException {

    public SeatUnavailableException(String message) {
        super(message);
    }
}
