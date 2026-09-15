package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Đơn hàng — bằng chứng "AI đã mua bao nhiêu vé, giá bao nhiêu".
 * Thiếu bảng này thì trừ kho xong không biết vé đi đâu.
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

    @Column(nullable = false)
    private Long ticketId;

    private int quantity;

    /** Giá 1 vé TẠI THỜI ĐIỂM MUA — giá vé sau này đổi thì đơn cũ không bị ảnh hưởng. */
    @Column(precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount;

    private int orderStatus;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
