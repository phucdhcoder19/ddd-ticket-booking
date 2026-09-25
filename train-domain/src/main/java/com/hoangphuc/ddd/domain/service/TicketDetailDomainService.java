package com.hoangphuc.ddd.domain.service;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import com.hoangphuc.ddd.domain.model.vo.SaleWindow;

import java.time.LocalDateTime;

public interface TicketDetailDomainService {
    TicketDetail getTicketDetailById(Long ticketId);
    int getStockAvailable(Long ticketId);
    boolean decreaseStock(Long ticketId, int quantity);
    boolean increaseStock(Long ticketId, int quantity);

    /**
     * Dot mo ban de HIEN THI: uu tien dot sap toi, de trang chu dem nguoc.
     * KHONG dung lam cong chan mua — xem isSaleOpen().
     */
    SaleWindow resolveSaleWindow(LocalDateTime now);

    /** Ngay luc nay co dang mo ban khong. Day moi la cong chan mua. */
    boolean isSaleOpen(LocalDateTime now);
}
