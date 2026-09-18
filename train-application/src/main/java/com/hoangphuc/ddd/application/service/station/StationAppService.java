package com.hoangphuc.ddd.application.service.station;

import com.hoangphuc.ddd.application.model.SaleWindowDTO;
import com.hoangphuc.ddd.application.model.StationDTO;

import java.util.List;

/** Dữ liệu tra cứu cho trang chủ: danh sách ga và đợt mở bán. */
public interface StationAppService {

    List<StationDTO> listStations();

    SaleWindowDTO getSaleWindow();
}
