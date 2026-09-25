package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Toa của một đoàn tàu — cũng là KHUÔN MẪU.
 *
 * "SE1 có 15 toa, toa 1-5 ngồi mềm, toa 6-10 khoang 6, toa 11-15 khoang 4."
 * Mô tả này đúng cho mọi ngày, nên lưu một lần.
 *
 * Ghế thật (bảng seat) mới gắn với từng chuyến, vì ghế có TRẠNG THÁI —
 * ghế 12 toa 3 của chuyến ngày 27 Tết đã bán, của ngày 28 thì chưa.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(
    name = "train_carriage",
    indexes = @Index(name = "idx_carriage_train", columnList = "trainId,number")
)
public class TrainCarriage {

    public static final String CLASS_SOFT_SEAT = "SOFT_SEAT";
    public static final String CLASS_BERTH_4   = "BERTH_4";
    public static final String CLASS_BERTH_6   = "BERTH_6";

    public static final String LAYOUT_SEAT_2_2 = "seat-2-2";
    public static final String LAYOUT_BERTH_4  = "berth-4";
    public static final String LAYOUT_BERTH_6  = "berth-6";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long trainId;

    /** Toa số mấy — khách đọc con số này trên vé. */
    private int number;

    /** SOFT_SEAT | BERTH_4 | BERTH_6 — khớp đúng SeatClassCode của frontend. */
    @Column(nullable = false, length = 16)
    private String seatClass;

    /** seat-2-2 | berth-4 | berth-6 — frontend dùng để vẽ sơ đồ. */
    @Column(nullable = false, length = 16)
    private String layout;

    /**
     * Toa ngồi: số hàng ghế (mỗi hàng 4 ghế, 2-2).
     * Toa nằm: số khoang.
     *
     * Tên trường là rowCount chứ không phải rows: "ROWS" là TỪ KHOÁ DÀNH
     * RIÊNG của MySQL 8 (dùng trong window function). Đặt tên cột là rows
     * thì Hibernate sinh ra câu CREATE TABLE mà MySQL từ chối, và lỗi chỉ
     * hiện lúc khởi động chứ không phải lúc biên dịch.
     */
    private int rowCount;

    /**
     * LUẬT NGHIỆP VỤ: một toa có bao nhiêu chỗ.
     * Suy ra từ layout chứ không lưu thành cột — lưu là có cơ hội lệch với rows.
     */
    public int seatCount() {
        return switch (layout) {
            case LAYOUT_SEAT_2_2 -> rowCount * 4;   // 2 ghế mỗi bên lối đi
            case LAYOUT_BERTH_4  -> rowCount * 4;   // 4 giường mỗi khoang
            case LAYOUT_BERTH_6  -> rowCount * 6;   // 6 giường mỗi khoang
            default -> 0;
        };
    }

    /** Số tầng giường. Toa ngồi trả 0. */
    public int berthLevels() {
        return switch (layout) {
            case LAYOUT_BERTH_4 -> 2;
            case LAYOUT_BERTH_6 -> 3;
            default -> 0;
        };
    }
}
