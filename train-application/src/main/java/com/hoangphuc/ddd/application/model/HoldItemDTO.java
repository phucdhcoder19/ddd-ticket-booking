package com.hoangphuc.ddd.application.model;

import lombok.Data;

/**
 * One seat in a hold, with its price.
 *
 * The price is recomputed from the hold's STORED JOURNEY (station pair + service
 * date), not from parameters sent by the client. So however often the customer
 * switches tabs, presses F5 or polls the countdown, they see the same number
 * they saw when picking.
 */
@Data
public class HoldItemDTO {

    /** Seat code "C3-12". */
    private String seatId;

    /** Seat number "12". */
    private String seatLabel;

    private int carriageNumber;

    private String seatClass;

    private long price;
}
