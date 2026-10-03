package com.hoangphuc.ddd.application.service.station;

import com.hoangphuc.ddd.application.model.SaleWindowDTO;
import com.hoangphuc.ddd.application.model.StationDTO;

import java.util.List;

/** Lookup data for the home page: the station list and the sale window. */
public interface StationAppService {

    List<StationDTO> listStations();

    SaleWindowDTO getSaleWindow();
}
