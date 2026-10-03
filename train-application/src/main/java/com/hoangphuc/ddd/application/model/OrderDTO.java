package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDTO {

    /**
     * Public identifier of the order. Uses the order number, not the
     * auto-increment id — same reason as holdCode: knowing id=41 lets you guess
     * someone else's order.
     */
    private String orderId;

    /** Order code printed for the customer. Currently the same as orderId. */
    private String code;

    /** PENDING | PAID | CANCELLED */
    private String status;

    private long totalAmount;

    private String tripId;
    private String fromCode;
    private String toCode;

    /** Seats bought. Empty for buy-by-quantity orders (lessons 19/21). */
    private List<HoldItemDTO> items;

    private LocalDateTime paidAt;
    private LocalDateTime createdAt;

    /** Why it failed — only set when the order was cancelled. */
    private String failureReason;
}
