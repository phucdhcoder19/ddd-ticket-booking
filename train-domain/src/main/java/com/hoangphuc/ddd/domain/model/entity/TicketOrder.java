package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Đơn hàng — bằng chứng "AI đã mua bao nhiêu vé, giá bao nhiêu".
 * Thiếu bảng này thì trừ kho xong không biết vé đi đâu.
 *
 * Phục vụ HAI luồng mua, nên một nửa số cột luôn rỗng ở mỗi luồng:
 *
 *   mua thẳng (bài 19/21)  -> ticketId + quantity + unitPrice
 *   đặt chỗ theo ghế       -> tripId + fromCode/toCode, ghế nằm ở seat.orderId
 *
 * Vì sao không tách hai bảng: đơn hàng là chứng từ thu tiền. Một bảng thì
 * "tổng doanh thu" là một câu SELECT; hai bảng thì mọi báo cáo về sau đều
 * phải UNION, và ai quên vế thứ hai là ra số sai mà không có gì báo.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(name = "ticket_order",
       indexes = @Index(name = "idx_ticket_order_user", columnList = "userId"))
public class TicketOrder {

    public static final int STATUS_PENDING   = 0;   // mới tạo, chờ thanh toán
    public static final int STATUS_PAID      = 1;
    public static final int STATUS_CANCELLED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Mã đơn trả cho khách. UNIQUE để không bao giờ trùng. */
    @Column(nullable = false, unique = true, length = 64)
    private String orderNumber;

    @Column(nullable = false)
    private Long userId;

    /** Luồng mua thẳng. NULL với đơn đặt theo ghế. */
    private Long ticketId;

    /** Luồng đặt theo ghế. NULL với đơn mua thẳng. */
    private Long tripId;

    /** Hành trình của khách — quyết định giá, nên phải lưu cùng đơn. */
    @Column(length = 8)
    private String fromCode;

    @Column(length = 8)
    private String toCode;

    private int quantity;

    /**
     * Giá 1 vé TẠI THỜI ĐIỂM MUA — giá vé sau này đổi thì đơn cũ không bị
     * ảnh hưởng. NULL với đơn đặt theo ghế: mỗi ghế một giá khác nhau
     * (tầng 1 đắt hơn tầng 3), không có con số "đơn giá" nào đúng cho cả đơn.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount;

    private int orderStatus;

    /** Thời điểm trả tiền xong. NULL khi đơn chưa thanh toán. */
    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
