package com.hoangphuc.ddd.application.service.order;

import com.hoangphuc.ddd.application.model.OrderResult;

public interface OrderAppService {

    /**
     * Turn a still-valid hold into an order.
     * On success, frees the admission (queueToken) so the next person in the waiting room can get in.
     */
    OrderResult createFromHold(String holdCode, Long userId, String queueToken);

    /** Only the owner's orders are found; anyone else gets ORDER_NOT_FOUND. */
    OrderResult getByOrderNumber(String orderNumber, Long userId);
}
