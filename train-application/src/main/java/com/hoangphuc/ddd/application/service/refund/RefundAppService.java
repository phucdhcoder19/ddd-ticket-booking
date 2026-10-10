package com.hoangphuc.ddd.application.service.refund;

import com.hoangphuc.ddd.application.model.RefundResult;

public interface RefundAppService {

    /** Refund one ticket (hold_passenger.id) of this user and put its seat back on sale. */
    RefundResult refund(Long userId, Long ticketId);
}
