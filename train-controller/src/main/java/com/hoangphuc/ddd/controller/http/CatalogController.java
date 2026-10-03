package com.hoangphuc.ddd.controller.http;

import com.hoangphuc.ddd.application.model.SaleWindowDTO;
import com.hoangphuc.ddd.application.model.StationDTO;
import com.hoangphuc.ddd.application.service.station.StationAppService;
import com.hoangphuc.ddd.controller.model.vo.ResultMessage;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Lookup data for the home page.
 *
 * Two endpoints in one controller because they are the same kind of thing:
 * read-only, almost never changing, no business logic. Two classes holding
 * one method each would be overkill.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class CatalogController {

    private final StationAppService stationAppService;

    @GetMapping("/stations")
    public ResultMessage<List<StationDTO>> getStations() {
        List<StationDTO> stations = stationAppService.listStations();
        log.info("[CONTROLLER] getStations | stations={}", stations.size());
        return ResultUtil.data(stations);
    }

    @GetMapping("/sale-window")
    public ResultMessage<SaleWindowDTO> getSaleWindow() {
        SaleWindowDTO window = stationAppService.getSaleWindow();
        log.info("[CONTROLLER] getSaleWindow | opensAt={} isOpen={}", window.getOpensAt(), window.isOpen());
        return ResultUtil.data(window);
    }
}
