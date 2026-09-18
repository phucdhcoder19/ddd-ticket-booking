package com.hoangphuc.ddd.infrastructure.persistence.mapper;

import com.hoangphuc.ddd.domain.model.entity.TicketDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TicketDetailJPAMapper extends JpaRepository<TicketDetail, Long> {

    @Query("SELECT t.stockAvailable FROM TicketDetail t WHERE t.id = :ticketId")
    Integer getStockAvailable(@Param("ticketId") Long ticketId);

    /**
     * Ve ACTIVE gan nhat CHUA toi gio mo ban -> dot sap mo.
     * Ten ham dai nhung Spring Data tu sinh ca cau SQL lan thu tu sap xep,
     * khong phai viet @Query.
     */
    Optional<TicketDetail> findFirstByStatusAndSaleStartTimeAfterOrderBySaleStartTimeAsc(
            int status, LocalDateTime now);

    /** Ve ACTIVE da toi gio mo gan day nhat -> dot dang chay (co the da het gio). */
    Optional<TicketDetail> findFirstByStatusAndSaleStartTimeLessThanEqualOrderBySaleStartTimeDesc(
            int status, LocalDateTime now);

    /**
     * TUYẾN PHÒNG THỦ 1 — CÁCH 1 trong 3 cách trừ kho MySQL (bài DDD 19).
     *
     * Điều kiện "AND t.stockAvailable >= :quantity" là thứ duy nhất
     * đảm bảo không oversell ở tầng DB:
     *   - InnoDB khoá dòng khi UPDATE -> các UPDATE cùng dòng phải xếp hàng
     *   - Người xếp sau kiểm tra điều kiện trên giá trị MỚI NHẤT
     *   - 1 câu SQL, không cần SELECT trước, không cần retry
     *
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
