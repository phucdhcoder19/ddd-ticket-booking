package com.hoangphuc.ddd.application.model;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class TicketDetailDTO {
    private Long id;
    private String name;
    private String description;
    private int stockAvailable;
    private BigDecimal price;        // already resolved: flash price if set, otherwise the original
    private boolean available;       // precomputed for the frontend, so it does not have to infer it
}
