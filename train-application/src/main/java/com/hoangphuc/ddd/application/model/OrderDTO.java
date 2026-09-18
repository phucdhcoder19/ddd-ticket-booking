package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderDTO {

    private String orderNumber;
    private Long ticketId;
    private String ticketName;
    private int quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;

    /** PENDING | PAID | CANCELLED */
    private String status;

    private LocalDateTime createdAt;
}
