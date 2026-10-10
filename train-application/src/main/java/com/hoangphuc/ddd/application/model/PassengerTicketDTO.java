package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * One ticket = one passenger on one seat of a paid order.
 * Field names match the frontend Ticket type exactly (src/api/types.ts).
 */
@Data
public class PassengerTicketDTO {

    /** hold_passenger.id — stable, and what a refund will point at later. */
    private String id;

    /** Ticket code printed for the customer: "<orderNumber>-<n>". */
    private String code;

    private String orderCode;
    private String trainCode;

    /** Station NAMES, ready to display. */
    private String fromStation;
    private String toStation;

    private LocalDateTime departAt;

    private int carriageNumber;
    private String seatLabel;
    private String seatClass;

    private String passengerName;

    /** Masked ID number (001•••••4567). The full number never leaves the server. */
    private String passengerId;

    /** Price after this passenger's discount — what was actually paid. */
    private long price;

    /** VALID | USED | REFUNDED */
    private String status;

    /** Money going back to the customer. NULL unless the ticket was refunded. */
    private Long refundAmount;

    private String qrPayload;
}
