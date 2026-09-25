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
        /** Không có chuyến đó, hoặc chuyến chưa sinh xong ghế. */
        TRIP_NOT_FOUND,
        /** Mã ga sai, hoặc ga đi trùng ga đến. */
        INVALID_ROUTE,
        /** Có người khác giành mất ít nhất một trong những chỗ đã chọn. */
        SEAT_TAKEN,
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
