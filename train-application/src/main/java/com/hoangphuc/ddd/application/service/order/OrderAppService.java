package com.hoangphuc.ddd.application.service.order;

import com.hoangphuc.ddd.application.model.OrderResult;

public interface OrderAppService {

    /** Đổi một lượt giữ chỗ còn hiệu lực thành đơn hàng. */
    OrderResult createFromHold(String holdCode);

    OrderResult getByOrderNumber(String orderNumber);
}
