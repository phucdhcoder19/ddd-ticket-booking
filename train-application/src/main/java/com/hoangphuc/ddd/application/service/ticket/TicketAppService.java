package com.hoangphuc.ddd.application.service.ticket;

import com.hoangphuc.ddd.application.model.TicketDetailDTO;

/**
 * Use case của nghiệp vụ vé.
 * Tầng này trả lời "làm theo thứ tự nào", không trả lời "luật là gì".
 */
public interface TicketAppService {

    TicketDetailDTO getTicketDetail(Long ticketId);

    /** @return "OK" nếu đặt được, "HET_VE" nếu không còn đủ vé */
    String buyTicket(Long ticketId, int quantity);
}
