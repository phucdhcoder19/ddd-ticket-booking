package com.hoangphuc.ddd.application.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/** One seat class on one trip: lowest price, how many left, how many in total. */
@Data
@AllArgsConstructor
public class SeatClassOfferDTO {
    /** SOFT_SEAT | BERTH_4 | BERTH_6 */
    private String code;
    /** LOWEST price of this class — the search screen shows "from ... VND". */
    private long price;
    private int available;
    private int total;
}
