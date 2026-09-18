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
 * Buoc cuoi cua vong doi: doi luot giu cho thanh don hang.
 *
 *   POST /orders                   { holdCode } -> tao don
 *   GET  /orders/{orderNumber}     tra cuu don
 */
@RestController
@RequestMapping("/orders")
@Slf4j
@RequiredArgsConstructor
public class OrderController {

    private final OrderAppService orderAppService;

    @PostMapping
    public ResultMessage<OrderDTO> create(@Valid @RequestBody CreateOrderRequest request) {
        return toResponse(orderAppService.createFromHold(request.getHoldCode()));
    }

    @GetMapping("/{orderNumber}")
    public ResultMessage<OrderDTO> get(@PathVariable("orderNumber") String orderNumber) {
        return toResponse(orderAppService.getByOrderNumber(orderNumber));
    }

    private ResultMessage<OrderDTO> toResponse(OrderResult result) {
        return switch (result.getStatus()) {
            case SUCCESS          -> ResultUtil.data(result.getOrder());
            case HOLD_NOT_FOUND   -> ResultUtil.error(404, "Khong tim thay luot giu cho");
            case TICKET_NOT_FOUND -> ResultUtil.error(404, "Khong tim thay ve");
            // 410 Gone: tung ton tai, gio mat roi -> client hien "Het gio giu cho"
            case HOLD_EXPIRED     -> ResultUtil.error(410, "Het gio giu cho, moi ban chon lai");
            case ERROR            -> ResultUtil.error(500, "Loi he thong, vui long thu lai");
        };
    }
}
