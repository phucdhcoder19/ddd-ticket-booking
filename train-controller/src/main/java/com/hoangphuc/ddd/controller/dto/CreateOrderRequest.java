package com.hoangphuc.ddd.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "holdCode khong duoc trong")
    private String holdCode;
}
