package com.hoangphuc.ddd.application.service.mytickets;

import com.hoangphuc.ddd.application.model.PassengerTicketDTO;

import java.util.List;

public interface MyTicketsAppService {

    /** Every ticket the user has bought, newest order first. */
    List<PassengerTicketDTO> listTickets(Long userId);
}
