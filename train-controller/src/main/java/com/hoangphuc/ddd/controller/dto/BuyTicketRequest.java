package com.hoangphuc.ddd.controller.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BuyTicketRequest {

    @NotNull(message = "ticketId khong duoc trong")
    private Long ticketId;

    @NotNull(message = "quantity khong duoc trong")
    @Min(value = 1, message = "quantity phai >= 1")
    private Integer quantity;
}
