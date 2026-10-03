package com.hoangphuc.ddd.application.service.ticket;

import com.hoangphuc.ddd.application.model.PlaceOrderResult;
import com.hoangphuc.ddd.application.model.TicketDetailDTO;

/**
 * Use cases of the ticket business.
 * This layer answers "in what order do things happen", not "what are the rules".
 */
public interface TicketAppService {

    TicketDetailDTO getTicketDetail(Long ticketId);

    /** Buy tickets: deduct stock + create the order, keeping data consistent. */
    PlaceOrderResult placeOrder(Long ticketId, Long userId, int quantity);
}
