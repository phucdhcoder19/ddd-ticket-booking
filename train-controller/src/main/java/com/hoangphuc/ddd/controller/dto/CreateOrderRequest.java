package com.hoangphuc.ddd.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "holdId is required")
    private String holdId;

    /**
     * VNPAY | MOMO | BANK_CARD.
     *
     * Accepted but NOT used yet: there is no real payment gateway, so an order
     * counts as paid as soon as it is created. It is declared here so the
     * frontend already sends the final shape, and so the place to change when
     * a gateway is plugged in is already part of the method signature.
     */
    private String paymentMethod;
}
