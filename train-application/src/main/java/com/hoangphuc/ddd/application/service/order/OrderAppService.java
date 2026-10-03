package com.hoangphuc.ddd.application.service.order;

import com.hoangphuc.ddd.application.model.OrderResult;

public interface OrderAppService {

    /**
     * Turn a still-valid hold into an order.
     * On success, frees the admission (queueToken) so the next person in the waiting room can get in.
     */
    OrderResult createFromHold(String holdCode, String queueToken);

    OrderResult getByOrderNumber(String orderNumber);
}
