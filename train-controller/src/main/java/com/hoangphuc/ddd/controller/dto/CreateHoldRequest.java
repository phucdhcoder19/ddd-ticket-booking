package com.hoangphuc.ddd.controller.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateHoldRequest {

    @NotNull(message = "ticketId khong duoc trong")
    private Long ticketId;

    // Chua co dang nhap nen client tu gui. Co auth roi thi lay tu token, KHONG tin client.
    @NotNull(message = "userId khong duoc trong")
    private Long userId;

    @NotNull(message = "quantity khong duoc trong")
    @Min(value = 1, message = "quantity phai >= 1")
    @Max(value = 4, message = "moi luot giu toi da 4 cho")
    private Integer quantity;
}
