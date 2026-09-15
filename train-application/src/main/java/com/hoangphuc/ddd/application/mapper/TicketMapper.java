package com.hoangphuc.ddd.application.mapper;

import com.hoangphuc.ddd.application.model.TicketDetailDTO;
import com.hoangphuc.ddd.domain.model.entity.TicketDetail;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Biên giới giữa domain và thế giới bên ngoài.
 * Entity đi vào, DTO đi ra — client không bao giờ nhìn thấy entity.
 */
public class TicketMapper {

    private TicketMapper() {
    }

    public static TicketDetailDTO toDTO(TicketDetail entity) {
        if (entity == null) {
            return null;
        }

        TicketDetailDTO dto = new TicketDetailDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setDescription(entity.getDescription());
        dto.setStockAvailable(entity.getStockAvailable());

        // Luật giá nằm ở domain (TicketDetail.effectivePrice), mapper chỉ gọi lại
        dto.setPrice(entity.effectivePrice());

        // Tính sẵn "còn bán được không" từ 3 field — FE không phải tự suy luận
        LocalDateTime now = LocalDateTime.now();
        dto.setAvailable(entity.getStatus() == 1
                && entity.getStockAvailable() > 0
                && (entity.getSaleEndTime() == null || now.isBefore(entity.getSaleEndTime())));

        return dto;
    }
}
