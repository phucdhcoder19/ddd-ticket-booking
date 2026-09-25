package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {

    /**
     * Dinh danh cong khai cua don. Bang dung ma don chu khong phai id tu
     * tang — cung ly do voi holdCode: biet id=41 la doan duoc don cua nguoi khac.
     */
    private String orderId;

    /** Ma don in cho khach doc. Hien tai trung voi orderId. */
    private String code;

    /** PENDING | PAID | CANCELLED */
    private String status;

    private long totalAmount;

    private String tripId;
    private String fromCode;
    private String toCode;

    /** Nhung cho da mua. Rong voi don mua thang (luong bai 19/21). */
    private List<HoldItemDTO> items;

    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    /** Vi sao that bai — chi co khi don bi huy. */
    private String failureReason;
}
