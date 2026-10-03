package com.hoangphuc.ddd.application.service.station.impl;

import com.hoangphuc.ddd.application.model.SaleWindowDTO;
import com.hoangphuc.ddd.application.model.StationDTO;
import com.hoangphuc.ddd.application.service.station.StationAppService;
import com.hoangphuc.ddd.domain.model.vo.SaleWindow;
import com.hoangphuc.ddd.domain.service.StationDomainService;
import com.hoangphuc.ddd.domain.service.TicketDetailDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class StationAppServiceImpl implements StationAppService {

    private final StationDomainService stationDomainService;
    private final TicketDetailDomainService ticketDetailDomainService;

    /**
     * The sale label is display TEXT, not business data — renaming a sale does
     * not change how tickets are sold. So it lives in config rather than in a
     * new table column.
     */
    @Value("${app.sale.label:Lunar New Year ticket sale}")
    private String saleLabel;

    @Override
    public List<StationDTO> listStations() {
        return stationDomainService.listStations().stream()
                .map(s -> {
                    StationDTO dto = new StationDTO();
                    dto.setCode(s.getCode());
                    dto.setName(s.getName());
                    dto.setRegion(s.getRegion());
                    return dto;
                })
                .toList();
    }

    @Override
    public SaleWindowDTO getSaleWindow() {
        LocalDateTime now = LocalDateTime.now();
        SaleWindow window = ticketDetailDomainService.resolveSaleWindow(now);

        SaleWindowDTO dto = new SaleWindowDTO();
        dto.setOpensAt(window.opensAt());
        dto.setOpen(window.open());
        dto.setLabel(saleLabel);
        dto.setServerNow(now);
        return dto;
    }
}
