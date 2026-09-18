package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * GIỮ CHỖ CÓ HẠN — trái tim của bài 18.
 *
 * Hold là "chỗ này đang có người xem, đừng bán cho ai khác": tạm thời, có
 * hạn, hết giờ thì biến mất. Khác hẳn TicketOrder — đơn hàng là chứng từ thu
 * tiền, tồn tại vĩnh viễn, không bao giờ được tự xoá.
 *
 * Tách hai thứ ra vì vòng đời khác nhau. Nhét chung một bảng thì bạn có một
 * đống dòng PENDING vừa là "đang xem" vừa là "đã mua chưa trả tiền" — không
 * phân biệt được, và báo cáo doanh thu phải lọc rác.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(
    name = "ticket_hold",
    indexes = {
        @Index(name = "uk_hold_code", columnList = "holdCode", unique = true),
        // Job quét đơn hết hạn chạy câu WHERE status = 0 AND expire_at < now.
        // Index ghép đúng thứ tự đó: lọc bằng cột đầu, so sánh khoảng bằng cột sau.
        @Index(name = "idx_hold_status_expire", columnList = "status,expireAt")
    }
)
public class Hold {

    public static final int STATUS_HOLDING  = 0;   // đang giữ, chưa quá hạn
    public static final int STATUS_USED     = 1;   // đã đổi thành đơn hàng
    public static final int STATUS_RELEASED = 2;   // đã trả kho (user huỷ hoặc hết giờ)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Mã công khai trả cho client. Không lộ id tự tăng ra ngoài — biết id=41
     * là đoán được id=42 của người khác.
     */
    @Column(nullable = false, unique = true, length = 40)
    private String holdCode;

    @Column(nullable = false)
    private Long ticketId;

    @Column(nullable = false)
    private Long userId;

    private int quantity;

    @Column(nullable = false)
    private int status;

    /** Thời điểm hết hạn giữ chỗ. Đây là NGUỒN SỰ THẬT, client chỉ đếm ngược theo. */
    @Column(nullable = false)
    private LocalDateTime expireAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * LUẬT NGHIỆP VỤ: còn giữ được không.
     * Nhận "now" làm tham số — cùng lý do với TicketDetail.isOpenedForSale():
     * test truyền được mốc bất kỳ, và Jackson không coi là field khi serialize.
     */
    public boolean isHolding(LocalDateTime now) {
        return status == STATUS_HOLDING && now.isBefore(expireAt);
    }

    /** Số giây còn lại, không bao giờ âm — client dùng để vẽ đồng hồ. */
    public long secondsLeft(LocalDateTime now) {
        if (status != STATUS_HOLDING) return 0;
        long s = Duration.between(now, expireAt).toSeconds();
        return Math.max(s, 0);
    }
}
