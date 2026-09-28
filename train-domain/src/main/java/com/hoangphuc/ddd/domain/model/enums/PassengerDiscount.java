package com.hoangphuc.ddd.domain.model.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Đối tượng được giảm giá vé.
 *
 * Nằm ở DOMAIN chứ không ở controller hay frontend: đây là luật nghiệp vụ
 * quyết định số tiền thu của khách. Frontend cũng có một bảng tỉ lệ y hệt
 * (DISCOUNT_RATE trong types.ts), nhưng bảng đó chỉ để HIỂN THỊ. Nếu server
 * tin con số client gửi lên thì ai sửa request cũng tự giảm được 100%.
 * Client chỉ được nói "tôi là sinh viên", còn giảm bao nhiêu thì server tính.
 */
public enum PassengerDiscount {

    NONE(0),
    STUDENT(10),
    CHILD(25),
    SENIOR(15);

    /** Phần trăm giảm. Dùng số nguyên để khỏi dính sai số của double (0.1 + 0.2 != 0.3). */
    private final int percent;

    PassengerDiscount(int percent) {
        this.percent = percent;
    }

    public int percent() {
        return percent;
    }

    /**
     * Giá sau giảm, làm tròn tới nghìn đồng — cùng quy tắc với finalPriceOf()
     * của frontend, để số khách thấy trên màn hình khớp với số bị trừ.
     *
     * BigDecimal + HALF_UP thay vì Math.round(double): tiền thì không được
     * phó mặc cho sai số dấu phẩy động, nhất là đúng ở mốc ...500đ, nơi lệch
     * một chút xíu là làm tròn sang hướng khác, chênh cả 1.000đ.
     */
    public long apply(long basePrice) {
        return BigDecimal.valueOf(basePrice)
                .multiply(BigDecimal.valueOf(100 - percent))
                .divide(BigDecimal.valueOf(100_000), 0, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(1_000))
                .longValueExact();
    }
}
