package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.util.List;

/** One carriage of a trip, with its seat map. */
@Data
public class CarriageDTO {

    private String id;

    /** Carriage number — passengers read this on the ticket. */
    private int number;

    /** SOFT_SEAT | BERTH_4 | BERTH_6 */
    private String seatClass;

    /** seat-2-2 | berth-4 | berth-6 — the frontend uses it to pick how to draw. */
    private String layout;

    /** Seating carriage: number of seat rows. Sleeper carriage: number of compartments. */
    private int rows;

    private List<SeatDTO> seats;

    /** How many places are free — lets the carriage tabs show it without recounting. */
    private int available;
}
