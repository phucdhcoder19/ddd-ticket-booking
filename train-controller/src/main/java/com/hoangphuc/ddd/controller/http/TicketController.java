package com.hoangphuc.ddd.controller.http;

import com.hoangphuc.ddd.application.model.PlaceOrderResult;
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
    public ResultMessage<PlaceOrderResult> buy(@Valid @RequestBody BuyTicketRequest request) {
        log.info("[CONTROLLER] buy | ticketId={} userId={} qty={}",
                request.getTicketId(), request.getUserId(), request.getQuantity());

        PlaceOrderResult result = ticketAppService.placeOrder(
                request.getTicketId(), request.getUserId(), request.getQuantity());

        // Chỉ controller mới biết mã HTTP
        return switch (result.getStatus()) {
            case SUCCESS          -> ResultUtil.data(result);
            case OUT_OF_STOCK     -> ResultUtil.error(409, "Het ve");
            case TICKET_NOT_FOUND -> ResultUtil.error(404, "Khong tim thay ve");
            case ERROR            -> ResultUtil.error(500, "Loi he thong, vui long thu lai");
        };
    }
}
