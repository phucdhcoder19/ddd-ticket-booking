package com.hoangphuc.ddd.application.service.trip;

import com.hoangphuc.ddd.application.model.TripDTO;

import java.time.LocalDate;
import java.util.List;

public interface TripAppService {

    /** Tim chuyen tau. Chuyen nao chua ton tai cho ngay do thi sinh luon. */
    List<TripDTO> search(String fromCode, String toCode, LocalDate date);
}
