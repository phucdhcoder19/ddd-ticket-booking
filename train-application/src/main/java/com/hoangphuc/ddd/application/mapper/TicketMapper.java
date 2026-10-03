package com.hoangphuc.ddd.application.mapper;

import com.hoangphuc.ddd.application.model.TicketDetailDTO;
import com.hoangphuc.ddd.domain.model.entity.TicketDetail;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * The boundary between the domain and the outside world.
 * Entity in, DTO out — the client never sees an entity.
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

        // The pricing rule lives in the domain (TicketDetail.effectivePrice); the mapper just calls it
        dto.setPrice(entity.effectivePrice());

        // "Can it still be bought" = EXACTLY the rule placeOrder() uses as its gate.
        // Call the domain instead of copying the condition here — one rule, defined in one place.
        LocalDateTime now = LocalDateTime.now();
        dto.setAvailable(entity.isOpenedForSale(now)
                && !entity.isSaleEnded(now)
                && entity.getStockAvailable() > 0);

        return dto;
    }
}
