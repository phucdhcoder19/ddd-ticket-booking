package com.hoangphuc.ddd.application.service.queue;

import com.hoangphuc.ddd.application.model.QueueTicketDTO;

public interface QueueAppService {

    /** Xin mot luot vao mua. */
    QueueTicketDTO join();

    /** Hoi lai xem toi luot chua. */
    QueueTicketDTO status(String token);
}
