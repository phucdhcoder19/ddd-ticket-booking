package com.hoangphuc.ddd.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BuyTicketRequest {

    @NotNull(message = "ticketId is required")
    private Long ticketId;

    // No login yet, so the client sends it. With auth, take it from the token and DO NOT trust the client.
    @NotNull(message = "userId is required")
    private Long userId;

    @NotNull(message = "quantity is required")
    @Min(value = 1, message = "quantity must be >= 1")
    private Integer quantity;
}
