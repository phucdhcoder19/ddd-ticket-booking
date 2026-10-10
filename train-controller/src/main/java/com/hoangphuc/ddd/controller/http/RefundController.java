package com.hoangphuc.ddd.controller.http;

import com.hoangphuc.ddd.application.model.PassengerTicketDTO;
import com.hoangphuc.ddd.application.model.RefundResult;
import com.hoangphuc.ddd.application.service.refund.RefundAppService;
import com.hoangphuc.ddd.controller.model.vo.ResultMessage;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import com.hoangphuc.ddd.controller.support.CurrentUser;
import com.hoangphuc.ddd.controller.support.IdempotentRequests;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 *   POST /tickets/{ticketId}/refund   refund one ticket of the caller
 *
 * "/tickets" (plural, seat booking) is not "/ticket" (TicketController, the
 * buy-by-quantity flash sale). The id is the one GET /me/tickets returns.
 */
@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class RefundController {

    private final RefundAppService refundAppService;
    private final IdempotentRequests idempotentRequests;

    @PostMapping("/{ticketId}/refund")
    public ResultMessage<PassengerTicketDTO> refund(@PathVariable("ticketId") Long ticketId,
                                                    @RequestHeader(value = IdempotentRequests.HEADER, required = false) String idempotencyKey) {
        // Money moves: a retry after a timeout must get "refunded" back, not
        // "this ticket was already refunded". There is no body, so the ticket
        // id is what the fingerprint covers.
        return idempotentRequests.run("refunds", idempotencyKey, Map.of("ticketId", ticketId),
                PassengerTicketDTO.class,
                () -> toResponse(refundAppService.refund(CurrentUser.id(), ticketId)));
    }

    private ResultMessage<PassengerTicketDTO> toResponse(RefundResult result) {
        return switch (result.getStatus()) {
            case SUCCESS          -> ResultUtil.data(result.getTicket());
            case TICKET_NOT_FOUND -> ResultUtil.error(404, "Ticket not found");
            // 422, not 409: the frontend reads 409 as "sold out". 422 shows this message as is.
            case ALREADY_REFUNDED -> ResultUtil.error(422, "This ticket has already been refunded");
            case TOO_LATE         -> ResultUtil.error(422, "Too close to departure, this ticket can no longer be refunded");
            case ERROR            -> ResultUtil.error(500, "System error, please try again");
        };
    }
}
