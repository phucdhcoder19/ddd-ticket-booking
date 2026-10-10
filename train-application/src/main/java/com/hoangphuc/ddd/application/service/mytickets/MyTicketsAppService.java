package com.hoangphuc.ddd.application.service.mytickets;

import com.hoangphuc.ddd.application.model.PassengerTicketDTO;

import java.util.List;
import java.util.Optional;

public interface MyTicketsAppService {

    /** Every ticket the user has bought, newest order first. */
    List<PassengerTicketDTO> listTickets(Long userId);

    /** One ticket (hold_passenger.id), only if it belongs to this user. */
    Optional<PassengerTicketDTO> findTicket(Long userId, Long ticketId);
}
