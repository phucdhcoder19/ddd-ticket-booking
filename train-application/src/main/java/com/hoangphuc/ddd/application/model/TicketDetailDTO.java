package com.hoangphuc.ddd.application.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TicketDetailDTO {
    private Long id;
    private String name;
    private String description;
    private int stockAvailable;
    private BigDecimal price;        // đã gộp: flash nếu có, không thì gốc
    private boolean available;       // đã tính sẵn cho FE, FE không phải suy luận
}
