package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class TripDTO {

    /** A String because the frontend uses it in URLs. */
    private String id;

    private String trainCode;

    /** Departure/arrival stations of the PASSENGER's journey, not of the train.
     *  SE1 runs Hanoi - Saigon, but a passenger may only ride Hue - Da Nang. */
    private StationDTO fromStation;
    private StationDTO toStation;

    private LocalDateTime departAt;
    private LocalDateTime arriveAt;
    private int durationMinutes;

    private List<SeatClassOfferDTO> classes;

    /** Free places on the whole trip — the frontend uses it for "Almost gone"/"Sold out" badges. */
    private int availableTotal;
}
