package com.hoangphuc.ddd.application.service.trip;

import com.hoangphuc.ddd.application.model.CarriageDTO;

import java.util.List;
import java.util.Optional;

public interface SeatMapAppService {

    /**
     * Seat map of a trip, only the carriages of the seat class the customer picked.
     *
     * Both stations are required: each seat's price depends on how far the
     * passenger travels, and the trip itself does not know where they get off.
     *
     * @return empty if there is no such trip, or a station code is invalid
     */
    Optional<List<CarriageDTO>> carriages(Long tripId, String seatClass, String fromCode, String toCode);
}
