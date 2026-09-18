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

        // "Còn bán được không" = ĐÚNG luật mà placeOrder() dùng để chặn.
        // Gọi lại domain thay vì chép điều kiện ra đây — một luật, một chỗ định nghĩa.
        LocalDateTime now = LocalDateTime.now();
        dto.setAvailable(entity.isOpenedForSale(now)
                && !entity.isSaleEnded(now)
                && entity.getStockAvailable() > 0);

        return dto;
    }
}
