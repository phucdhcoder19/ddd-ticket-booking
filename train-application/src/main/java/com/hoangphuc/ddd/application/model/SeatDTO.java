package com.hoangphuc.ddd.application.model;

import lombok.Data;

/** One place on a carriage seat map. Field names match the frontend Seat type exactly. */
@Data
public class SeatDTO {

    /** Seat code, e.g. "C3-12". The frontend uses this very string as the id when holding. */
    private String id;

    /** Number printed on the ticket: "12". */
    private String label;

    private int row;
    private int col;

    /**
     * Sleeper carriage: compartment number. Seating carriage: NULL.
     *
     * Null rather than 0: the frontend declares compartment?, meaning "this
     * carriage has no notion of compartments". 0 would be a compartment named 0.
     */
    private Integer compartment;

    /** Berth level 1..3. Seating carriage: NULL. */
    private Integer berthLevel;

    /** available | held | sold */
    private String status;

    private long price;
}
