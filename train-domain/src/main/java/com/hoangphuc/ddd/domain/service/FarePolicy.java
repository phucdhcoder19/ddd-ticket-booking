package com.hoangphuc.ddd.domain.service;

import com.hoangphuc.ddd.domain.model.entity.TrainCarriage;

/**
 * LUẬT TÍNH GIÁ VÉ.
 *
 * Giá = quãng đường × đơn giá theo hạng chỗ × hệ số tầng giường × phụ thu cao điểm
 *
 * Bốn thừa số, và ba trong số đó chỉ biết được LÚC KHÁCH TÌM CHUYẾN:
 * quãng đường phụ thuộc cặp ga, phụ thu phụ thuộc ngày đi. Đó là lý do bảng
 * seat không có cột giá — lưu giá vào ghế là lưu một con số chỉ đúng cho
 * một cặp ga và một ngày.
 */
public final class FarePolicy {

    private FarePolicy() {
    }

    /** Đơn giá cơ bản, đồng trên mỗi ki-lô-mét. */
    private static long pricePerKm(String seatClass) {
        return switch (seatClass) {
            case TrainCarriage.CLASS_SOFT_SEAT -> 620L;
            case TrainCarriage.CLASS_BERTH_6   -> 900L;
            case TrainCarriage.CLASS_BERTH_4   -> 1180L;
            default -> 0L;
        };
    }

    /**
     * Hệ số theo tầng giường. Tầng 1 thấp nhất, dễ lên xuống, để đồ tiện —
     * nên đắt nhất. Càng lên cao càng rẻ.
     */
    private static double berthFactor(int berthLevel) {
        return switch (berthLevel) {
            case 1 -> 1.00;
            case 2 -> 0.92;
            case 3 -> 0.85;
            default -> 1.00;   // toa ngồi, không có tầng
        };
    }

    /**
     * @param distanceKm     quãng đường giữa ga đi và ga đến
     * @param seatClass      hạng chỗ
     * @param berthLevel     tầng giường, 0 nếu là toa ngồi
     * @param peakSurcharge  hệ số cao điểm, 1.0 nếu ngày thường
     * @return giá vé, đã làm tròn tới nghìn đồng
     */
    public static long fare(int distanceKm, String seatClass, int berthLevel, double peakSurcharge) {
        double raw = (double) distanceKm
                * pricePerKm(seatClass)
                * berthFactor(berthLevel)
                * peakSurcharge;
        // Làm tròn nghìn: giá vé không bao giờ hiển thị lẻ tới đồng
        return Math.round(raw / 1000d) * 1000L;
    }
}
