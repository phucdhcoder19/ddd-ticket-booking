package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketDetailJPAMapper extends JpaRepository<TicketDetail, Long> {

    @Query("SELECT t.stockAvailable FROM TicketDetail t WHERE t.id = :ticketId")
    Integer getStockAvailable(@Param("ticketId") Long ticketId);

    /**
     * TUYẾN PHÒNG THỦ 1.
     * Điều kiện "AND t.stockAvailable >= :quantity" là thứ duy nhất
     * đảm bảo không oversell ở tầng DB — MySQL sẽ lock row khi UPDATE.
     * Trả về số row bị ảnh hưởng: 1 = trừ được, 0 = hết vé.
     */
    @Modifying
    @Query("UPDATE TicketDetail t SET t.stockAvailable = t.stockAvailable - :quantity, " +
           "t.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE t.id = :ticketId AND t.stockAvailable >= :quantity")
    int decreaseStock(@Param("ticketId") Long ticketId, @Param("quantity") int quantity);

    @Modifying
    @Query("UPDATE TicketDetail t SET t.stockAvailable = t.stockAvailable + :quantity, " +
           "t.updatedAt = CURRENT_TIMESTAMP " +
           "WHERE t.id = :ticketId")
    int increaseStock(@Param("ticketId") Long ticketId, @Param("quantity") int quantity);
}
