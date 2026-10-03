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
     * The nearest ACTIVE ticket whose sale has NOT opened yet -> the upcoming sale.
     * The method name is long, but Spring Data generates the whole query,
     * including the sort order, without a @Query.
     */
    Optional<TicketDetail> findFirstByStatusAndSaleStartTimeAfterOrderBySaleStartTimeAsc(
            int status, LocalDateTime now);

    /** The ACTIVE ticket that opened most recently -> the running sale (may have ended). */
    Optional<TicketDetail> findFirstByStatusAndSaleStartTimeLessThanEqualOrderBySaleStartTimeDesc(
            int status, LocalDateTime now);

    /**
     * FIRST LINE OF DEFENCE — OPTION 1 of the 3 ways to deduct MySQL stock (DDD lesson 19).
     *
     * The condition "AND t.stockAvailable >= :quantity" is the only thing that
     * prevents overselling at the DB level:
     *   - InnoDB locks the row on UPDATE -> UPDATEs on the same row must queue
     *   - Whoever comes later checks the condition against the LATEST value
     *   - 1 SQL statement, no SELECT first, no retry
     *
     * Returns the number of affected rows: 1 = deducted, 0 = sold out.
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
