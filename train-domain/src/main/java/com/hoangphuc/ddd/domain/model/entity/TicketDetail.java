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

    private int stockInitial;       // total tickets put on sale
    private int stockAvailable;     // tickets left  ← what the whole lesson revolves around

    private BigDecimal priceOriginal;
    private BigDecimal priceFlash;

    private LocalDateTime saleStartTime;
    private LocalDateTime saleEndTime;

    private int status;             // 0=INACTIVE, 1=ACTIVE, 2=DELETED
    private Long activityId;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * BUSINESS RULE: actual selling price = flash price if set, otherwise the original price.
     * No "get" prefix so Jackson does NOT treat it as a field when caching in Redis.
     */
    public BigDecimal effectivePrice() {
        if (priceFlash != null && priceFlash.compareTo(BigDecimal.ZERO) > 0) {
            return priceFlash;
        }
        return priceOriginal;
    }

    /**
     * BUSINESS RULE: is the ticket on sale yet?
     * It must be ACTIVE and past the opening time. saleStartTime null = on sale immediately.
     *
     * Takes "now" as a parameter instead of calling LocalDateTime.now() inside:
     *   1. Tests can pass any instant
     *   2. A method WITH parameters is not treated as a field by Jackson when
     *      caching in Redis (same reason effectivePrice() is not getEffectivePrice())
     */
    public boolean isOpenedForSale(LocalDateTime now) {
        if (status != STATUS_ACTIVE) {
            return false;
        }
        return saleStartTime == null || !now.isBefore(saleStartTime);
    }

    /** BUSINESS RULE: has the sale closed? saleEndTime null = never closes. */
    public boolean isSaleEnded(LocalDateTime now) {
        return saleEndTime != null && now.isAfter(saleEndTime);
    }
}
