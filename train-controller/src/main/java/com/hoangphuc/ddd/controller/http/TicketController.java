package com.hoangphuc.ddd.controller.http;

import com.hoangphuc.ddd.application.model.TicketDetailDTO;
import com.hoangphuc.ddd.application.service.ticket.TicketAppService;
import com.hoangphuc.ddd.controller.dto.BuyTicketRequest;
import com.hoangphuc.ddd.controller.model.vo.ResultMessage;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ticket")
@Slf4j
@RequiredArgsConstructor
public class TicketController {

    private final TicketAppService ticketAppService;

    @GetMapping("/{ticketId}")
    public ResultMessage<TicketDetailDTO> getTicket(@PathVariable("ticketId") Long ticketId) {
        log.info("[CONTROLLER] getTicket | ticketId={}", ticketId);

        TicketDetailDTO dto = ticketAppService.getTicketDetail(ticketId);
        if (dto == null) {
            return ResultUtil.error(404, "Khong tim thay ve");
        }
        return ResultUtil.data(dto);
    }

    @PostMapping("/buy")
    public ResultMessage<String> buy(@Valid @RequestBody BuyTicketRequest request) {
        log.info("[CONTROLLER] buy | ticketId={} qty={}",
                request.getTicketId(), request.getQuantity());
        try {
            String result = ticketAppService.buyTicket(request.getTicketId(), request.getQuantity());
            if ("HET_VE".equals(result)) {
                // 409 Conflict — chỉ controller mới được biết tới mã HTTP
                return ResultUtil.error(409, "Het ve");
            }
            return ResultUtil.data(result);
        } catch (Exception e) {
            log.error("[CONTROLLER] buy loi he thong | ticketId={}", request.getTicketId(), e);
            return ResultUtil.error(500, "Loi he thong");
        }
    }
}
