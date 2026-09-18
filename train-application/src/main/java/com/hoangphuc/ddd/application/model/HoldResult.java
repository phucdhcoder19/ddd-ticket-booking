package com.hoangphuc.ddd.application.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Kết quả giữ chỗ. Không có mã HTTP ở đây — đổi Status sang 200/409/410
 * là việc của controller.
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class HoldResult {

    public enum Status {
        SUCCESS,
        OUT_OF_STOCK,
        TICKET_NOT_FOUND,
        NOT_ON_SALE,
        SALE_ENDED,
        HOLD_NOT_FOUND,
        HOLD_EXPIRED,     // đã quá hạn hoặc đã bị đóng
        ERROR
    }

    private final Status status;
    private final HoldDTO hold;

    public static HoldResult success(HoldDTO hold) {
        return new HoldResult(Status.SUCCESS, hold);
    }

    public static HoldResult fail(Status status) {
        return new HoldResult(status, null);
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
}
