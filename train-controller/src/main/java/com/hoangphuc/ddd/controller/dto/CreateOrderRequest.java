package com.hoangphuc.ddd.controller.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrderRequest {

    @NotBlank(message = "holdId khong duoc trong")
    private String holdId;

    /**
     * VNPAY | MOMO | BANK_CARD.
     *
     * Nhan vao nhung CHUA dung: chua co cong thanh toan that, don duoc coi
     * nhu da tra tien ngay khi tao. Van khai bao o day de frontend gui dung
     * hinh dang cuoi cung ngay tu bay gio, va de cho nao can sua khi cam
     * cong thanh toan vao thi da nam san trong chu ky ham.
     */
    private String paymentMethod;
}
