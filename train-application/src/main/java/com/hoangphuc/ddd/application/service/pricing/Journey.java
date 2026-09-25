package com.hoangphuc.ddd.application.service.pricing;

import com.hoangphuc.ddd.domain.model.entity.Station;

/**
 * HÀNH TRÌNH CỦA KHÁCH: đi từ đâu, tới đâu, dài bao nhiêu, ngày đó có phụ thu không.
 *
 * Ba tầng đều cần đúng bốn con số này để ra được một giá vé: màn tìm chuyến
 * (giá "từ ...đ"), màn sơ đồ ghế (giá từng chỗ), và lúc giữ chỗ (tổng tiền).
 * Gói lại thành một kiểu để không nơi nào phải tự đi tra ga rồi trừ cây số —
 * ba chỗ tự tính là ba cơ hội ra ba con số khác nhau cho cùng một chuyến.
 */
public record Journey(Station from, Station to, int distanceKm, double surcharge) {
}
