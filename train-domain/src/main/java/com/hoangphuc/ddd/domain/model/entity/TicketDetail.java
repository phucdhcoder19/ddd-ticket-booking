package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
@Entity
@Table(name = "ticket_item")
public class TicketDetail {

    public static final int STATUS_INACTIVE = 0;
    public static final int STATUS_ACTIVE   = 1;
    public static final int STATUS_DELETED  = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String description;

    private int stockInitial;       // tổng vé mở bán
    private int stockAvailable;     // vé còn lại  ← thứ cả bài học xoay quanh

    private BigDecimal priceOriginal;
    private BigDecimal priceFlash;

    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;

    private int status;             // 0=INACTIVE, 1=ACTIVE, 2=DELETED
    private Long activityId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * LUẬT NGHIỆP VỤ: giá bán thực tế = giá flash nếu có, không thì giá gốc.
     * Tên không có tiền tố "get" để Jackson KHÔNG coi là field khi cache vào Redis.
     */
    public BigDecimal effectivePrice() {
        if (priceFlash != null && priceFlash.compareTo(BigDecimal.ZERO) > 0) {
            return priceFlash;
        }
        return priceOriginal;
    }

    /**
     * LUẬT NGHIỆP VỤ: vé đã được mở bán chưa.
     * Phải ACTIVE và đã tới giờ mở. saleStartTime null = mở bán ngay.
     *
     * Nhận "now" làm tham số, không tự gọi LocalDateTime.now() bên trong:
     *   1. Test truyền được mốc thời gian bất kỳ
     *   2. Hàm CÓ tham số thì Jackson không coi là field khi cache vào Redis
     *      (cùng lý do đặt tên effectivePrice() thay vì getEffectivePrice())
     */
    public boolean isOpenedForSale(LocalDateTime now) {
        if (status != STATUS_ACTIVE) {
            return false;
        }
        return saleStartTime == null || !now.isBefore(saleStartTime);
    }

    /** LUẬT NGHIỆP VỤ: đã qua giờ đóng bán chưa. saleEndTime null = bán vô hạn. */
    public boolean isSaleEnded(LocalDateTime now) {
        return saleEndTime != null && now.isAfter(saleEndTime);
    }
}
