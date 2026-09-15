package com.hoangphuc.ddd.application.service.ticket;

import com.hoangphuc.ddd.application.model.PlaceOrderResult;
import com.hoangphuc.ddd.application.model.TicketDetailDTO;

/**
 * Use case của nghiệp vụ vé.
 * Tầng này trả lời "làm theo thứ tự nào", không trả lời "luật là gì".
 */
public interface TicketAppService {

    TicketDetailDTO getTicketDetail(Long ticketId);

    /** Đặt vé: trừ kho + tạo đơn, nhất quán dữ liệu. */
    PlaceOrderResult placeOrder(Long ticketId, Long userId, int quantity);
}
