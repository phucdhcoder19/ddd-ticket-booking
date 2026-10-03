package com.hoangphuc.ddd.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class CreateHoldRequest {

    @NotNull(message = "tripId is required")
    private Long tripId;

    @NotBlank(message = "seatClass is required")
    private String seatClass;

    /**
     * Seat codes: ["C11-3", "C11-4"].
     *
     * At most 4 seats per hold — a business limit against scalpers, and also a
     * technical one: the more seats in one request, the more likely it collides
     * with someone else, and in this business one lost seat fails the whole hold.
     */
    @NotEmpty(message = "pick at least 1 seat")
    @Size(max = 4, message = "a hold has at most 4 seats")
    private List<String> seatIds;

    /**
     * The passenger's journey. Required because the PRICE depends on distance:
     * the same lower berth in carriage 11 costs very differently on
     * Hanoi - Vinh and Hanoi - Saigon.
     */
    @NotBlank(message = "departure station is required")
    private String from;

    @NotBlank(message = "arrival station is required")
    private String to;
}
