package com.hoangphuc.ddd.application.model;

import com.hoangphuc.ddd.domain.model.enums.PassengerDiscount;

/**
 * One passenger's details, ALREADY NORMALISED by the controller: whitespace
 * in the name collapsed, phone number in the 0xxxxxxxxx form. The layers
 * below no longer worry about formatting, only about business rules.
 */
public record PassengerCommand(
        String seatId,
        String fullName,
        String idNumber,
        String phone,
        PassengerDiscount discount) {
}
