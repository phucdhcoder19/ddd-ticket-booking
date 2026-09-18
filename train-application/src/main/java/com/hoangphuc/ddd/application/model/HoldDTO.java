package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class HoldDTO {

    private String holdCode;
    private Long ticketId;
    private String ticketName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;

    /** Hết hạn lúc nào — client vẽ đồng hồ từ mốc này. */
    private LocalDateTime expiresAt;

    /** Giờ SERVER. Client phải trừ theo giờ này, không theo giờ máy mình. */
    private LocalDateTime serverNow;

    /** Tính sẵn cho client đỡ phải tự trừ hai mốc thời gian. */
    private long secondsLeft;

    /** HOLDING | USED | RELEASED | EXPIRED */
    private String status;
}
