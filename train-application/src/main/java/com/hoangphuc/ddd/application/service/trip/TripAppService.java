package com.hoangphuc.ddd.application.service.trip;

import com.hoangphuc.ddd.application.model.TripDTO;

import java.time.LocalDate;
import java.util.List;

public interface TripAppService {

    /** Search trips. Any trip that does not exist yet for that date is provisioned on the spot. */
    List<TripDTO> search(String fromCode, String toCode, LocalDate date);
}
