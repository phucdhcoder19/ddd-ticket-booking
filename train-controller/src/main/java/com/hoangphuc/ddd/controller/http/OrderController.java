package com.hoangphuc.ddd.controller.http;

import com.hoangphuc.ddd.application.model.OrderDTO;
import com.hoangphuc.ddd.application.model.OrderResult;
import com.hoangphuc.ddd.application.service.order.OrderAppService;
import com.hoangphuc.ddd.controller.dto.CreateOrderRequest;
import com.hoangphuc.ddd.controller.model.vo.ResultMessage;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * Last step of the lifecycle: turn a hold into an order.
 *
 *   POST /orders                   { holdId } -> create the order
 *   GET  /orders/{orderNumber}     look up an order
 */
@RestController
@RequestMapping("/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderAppService orderAppService;

    @PostMapping
    public ResultMessage<OrderDTO> create(@Valid @RequestBody CreateOrderRequest request,
                                          @RequestHeader(value = HoldController.QUEUE_TOKEN_HEADER, required = false) String queueToken) {
        return toResponse(orderAppService.createFromHold(request.getHoldId(), queueToken));
    }

    @GetMapping("/{orderNumber}")
    public ResultMessage<OrderDTO> get(@PathVariable("orderNumber") String orderNumber) {
        return toResponse(orderAppService.getByOrderNumber(orderNumber));
    }

    private ResultMessage<OrderDTO> toResponse(OrderResult result) {
        return switch (result.getStatus()) {
            case SUCCESS          -> ResultUtil.data(result.getOrder());
            case HOLD_NOT_FOUND   -> ResultUtil.error(404, "Hold not found");
            case ORDER_NOT_FOUND  -> ResultUtil.error(404, "Order not found");
            case TICKET_NOT_FOUND -> ResultUtil.error(404, "Ticket not found");
            // 410 Gone: existed, now gone -> the client shows "Your hold has expired"
            case HOLD_EXPIRED     -> ResultUtil.error(410, "Your hold has expired, please pick your seats again");
            // 422, not 409: the frontend reads 409 as "sold out", while this is
            // incomplete data — the seats are still held, the customer just goes back and fills it in.
            case PASSENGERS_MISSING -> ResultUtil.error(422, "Passenger details are missing for some seats");
            case ERROR            -> ResultUtil.error(500, "System error, please try again");
        };
    }
}
