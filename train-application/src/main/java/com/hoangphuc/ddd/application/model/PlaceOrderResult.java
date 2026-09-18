package com.hoangphuc.ddd.application.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

/**
 * Kết quả đặt vé trả từ tầng application.
 * Không có mã HTTP ở đây — đổi Status sang 200/409/... là việc của controller.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlaceOrderResult {

    public enum Status { SUCCESS, OUT_OF_STOCK, TICKET_NOT_FOUND, NOT_ON_SALE, SALE_ENDED, ERROR }

    private final Status status;
    private final String orderNumber;
    private final Integer quantity;
    private final BigDecimal totalAmount;

    public static PlaceOrderResult success(String orderNumber, int quantity, BigDecimal totalAmount) {
        return new PlaceOrderResult(Status.SUCCESS, orderNumber, quantity, totalAmount);
    }

    public static PlaceOrderResult fail(Status status) {
        return new PlaceOrderResult(status, null, null, null);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
