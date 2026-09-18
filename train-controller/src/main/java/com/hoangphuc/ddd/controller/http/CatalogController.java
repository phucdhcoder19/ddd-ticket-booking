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
 * Dữ liệu tra cứu cho trang chủ.
 *
 * Gộp hai endpoint vào một controller vì chúng cùng một loại: chỉ đọc, gần
 * như không đổi, không có nghiệp vụ. Tách thành hai class chỉ để mỗi class
 * giữ một hàm là thừa.
 */
@RestController
@Slf4j
@RequiredArgsConstructor
public class CatalogController {

    private final StationAppService stationAppService;

    @GetMapping("/stations")
    public ResultMessage<List<StationDTO>> getStations() {
        List<StationDTO> stations = stationAppService.listStations();
        log.info("[CONTROLLER] getStations | so ga={}", stations.size());
        return ResultUtil.data(stations);
    }

    @GetMapping("/sale-window")
    public ResultMessage<SaleWindowDTO> getSaleWindow() {
        SaleWindowDTO window = stationAppService.getSaleWindow();
        log.info("[CONTROLLER] getSaleWindow | opensAt={} isOpen={}", window.getOpensAt(), window.isOpen());
        return ResultUtil.data(window);
    }
}
