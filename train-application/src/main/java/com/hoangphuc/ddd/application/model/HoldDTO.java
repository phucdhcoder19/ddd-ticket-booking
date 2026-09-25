package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class HoldDTO {

    /** Ma cong khai cua luot giu cho. Khong bao gio la id tu tang. */
    private String holdId;

    private String tripId;

    private String fromCode;
    private String toCode;

    /** Nhung cho dang giu, kem gia tung cho. */
    private List<HoldItemDTO> items;

    /** Tong tien tam tinh — chua tru giam gia cua tung hanh khach. */
    private long totalAmount;

    /** Het han luc nao — client ve dong ho tu moc nay. */
    private LocalDateTime expiresAt;

    /** Gio SERVER. Client phai tru theo gio nay, khong theo gio may minh. */
    private LocalDateTime serverNow;

    /** Tinh san cho client do phai tu tru hai moc thoi gian. */
    private long secondsLeft;

    /** HOLDING | USED | RELEASED | EXPIRED */
    private String status;
}
