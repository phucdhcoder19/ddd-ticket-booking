package com.hoangphuc.ddd.application.service.ticket;

/**
 * The hold does not have a passenger name for every seat yet.
 *
 * Thrown from INSIDE the hold -> order transaction, AFTER markUsed() has run:
 * throwing makes Spring roll back, the hold returns to status 0 and the
 * customer still has time to go back to the details screen. Returning null
 * would leave the hold marked "used" with no order — the customer loses the
 * seats and cannot buy them again.
 */
public class PassengersMissingException extends RuntimeException {

    public PassengersMissingException(String message) {
        super(message);
    }
}
