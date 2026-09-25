package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * Một chỗ ngồi/nằm của MỘT chuyến cụ thể.
 *
 * Đây là đơn vị tồn kho thật của nghiệp vụ tàu. Con số "còn 997 vé" chỉ là
 * tổng hợp đếm được từ bảng này, không phải nguồn sự thật.
 *
 * Vì sao ghế phải gắn với chuyến chứ không gắn với toa: ghế 12 toa 3 của
 * chuyến ngày 27 Tết đã bán, của ngày 28 thì chưa. Trạng thái khác nhau
 * theo ngày, nên phải có một dòng cho mỗi (chuyến, ghế).
 *
 * KHÔNG lưu giá ở đây. Giá phụ thuộc quãng đường khách đi (Hà Nội → Huế
 * khác Hà Nội → Sài Gòn), mà quãng đường chỉ biết được lúc khách tìm chuyến.
 * Lưu giá vào ghế là lưu một con số chỉ đúng cho một cặp ga.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(
    name = "seat",
    uniqueConstraints = @UniqueConstraint(name = "uk_seat_trip_code", columnNames = {"tripId", "seatCode"}),
    indexes = {
        // Câu hỏi nóng nhất: "toa này của chuyến này còn ghế nào trống?"
        @Index(name = "idx_seat_trip_class_status", columnList = "tripId,seatClass,status"),
        // Job thu hồi: "ghế nào đang bị hold này giữ?"
        @Index(name = "idx_seat_hold", columnList = "holdId"),
        // In vé: "đơn này gồm những ghế nào?"
        @Index(name = "idx_seat_order", columnList = "orderId")
    }
)
public class Seat {

    public static final int STATUS_FREE = 0;
    public static final int STATUS_HELD = 1;
    public static final int STATUS_SOLD = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    /** Mã hiển thị cho khách và cho frontend: "C3-12" = toa 3, chỗ 12. */
    @Column(nullable = false, length = 16)
    private String seatCode;

    private int carriageNumber;

    @Column(nullable = false, length = 16)
    private String seatClass;

    /** Số hiệu chỗ trong toa, in trên vé: "12". */
    @Column(nullable = false, length = 8)
    private String label;

    private int rowNo;
    private int colNo;

    /** Toa nằm: số khoang (1..n). Toa ngồi: 0. */
    private int compartment;

    /** Tầng giường: 1 (thấp, đắt nhất) .. 3. Toa ngồi: 0. */
    private int berthLevel;

    @Column(nullable = false)
    private int status;

    /** Lượt giữ chỗ đang chiếm ghế này. NULL khi ghế trống hoặc đã bán. */
    private Long holdId;

    /**
     * Đơn hàng đã mua ghế này. NULL khi ghế chưa bán.
     *
     * Vì sao ghế phải nhớ đơn chứ không chỉ nhớ "đã bán": in vé cần biết chỗ
     * này thuộc đơn nào, và hoàn vé cần trả đúng chỗ đó về kho. Chỉ có
     * status = 2 thì biết ghế đã bán nhưng không biết bán cho ai.
     */
    private Long orderId;

    public boolean isFree() {
        return status == STATUS_FREE;
    }

    /** Tên trạng thái đúng như kiểu SeatStatus của frontend. */
    public String statusName() {
        return switch (status) {
            case STATUS_HELD -> "held";
            case STATUS_SOLD -> "sold";
            default -> "available";
        };
    }
}
