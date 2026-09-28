package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * Người sẽ ngồi trên MỘT ghế của một lượt giữ chỗ.
 *
 * Vé tàu là vé ghi tên: soát vé đối chiếu họ tên và CCCD in trên vé với giấy
 * tờ thật. Nên mỗi ghế phải có đúng một hành khách trước khi được thành đơn.
 *
 * Vì sao là bảng riêng, không thêm cột vào seat: seat là TỒN KHO của chuyến,
 * dòng của nó sống mãi và được bán lại khi lượt giữ hết hạn. Ghi tên người
 * lên ghế thì hold hết giờ xong phải nhớ xoá tên, quên là người mua sau
 * thấy tên người trước. Ở bảng riêng, hold hết hạn thì mấy dòng này đơn giản
 * là không còn ai dùng tới.
 *
 * GIÁ ĐƯỢC CHỐT Ở ĐÂY. basePrice là giá ghế theo quãng đường, finalPrice là
 * sau khi trừ giảm giá của đúng người này. Đơn hàng cộng finalPrice, không
 * tính lại, cùng lý do với totalAmount trên Hold.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(
    name = "hold_passenger",
    uniqueConstraints = @UniqueConstraint(name = "uk_hold_passenger_seat", columnNames = {"holdId", "seatCode"}),
    indexes = {
        // "Vé của tôi" và in vé: đơn này gồm những hành khách nào.
        @Index(name = "idx_hold_passenger_order", columnList = "orderId")
    }
)
public class HoldPassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long holdId;

    /** Mã ghế trong chuyến: "C3-12". Cùng mã với seat.seatCode. */
    @Column(nullable = false, length = 16)
    private String seatCode;

    @Column(nullable = false, length = 100)
    private String fullName;

    /** CCCD 12 số, hoặc CMND cũ 9 số. */
    @Column(nullable = false, length = 12)
    private String idNumber;

    /** Đã chuẩn hoá về dạng 0xxxxxxxxx. */
    @Column(nullable = false, length = 10)
    private String phone;

    /** Tên của PassengerDiscount. Lưu chuỗi, không lưu số thứ tự: thêm một loại vào giữa enum là số cũ sai hết. */
    @Column(nullable = false, length = 16)
    private String discount;

    private long basePrice;
    private long finalPrice;

    /** Đơn hàng đã mua vé này. NULL cho tới khi hold được đổi thành đơn. */
    private Long orderId;

    private LocalDateTime createdAt;
}
