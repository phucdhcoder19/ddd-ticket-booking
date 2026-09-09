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
}
