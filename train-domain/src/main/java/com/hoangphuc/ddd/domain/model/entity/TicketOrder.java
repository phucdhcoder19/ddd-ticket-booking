package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * An order — the record of "WHO bought how many tickets, at what price".
 * Without this table, stock goes down and nobody knows where the tickets went.
 *
 * Serves TWO purchase flows, so half of the columns are always empty in each:
 *
 *   buy by quantity (lessons 19/21)  -> ticketId + quantity + unitPrice
 *   book by seat                     -> tripId + fromCode/toCode, seats live in seat.orderId
 *
 * Why not two tables: an order is a payment record. With one table, "total
 * revenue" is one SELECT; with two, every future report needs a UNION, and
 * whoever forgets the second half gets a wrong number with no warning.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(name = "ticket_order",
       indexes = @Index(name = "idx_ticket_order_user", columnList = "userId"))
public class TicketOrder {

    public static final int STATUS_PENDING   = 0;   // just created, waiting for payment
    public static final int STATUS_PAID      = 1;
    public static final int STATUS_CANCELLED = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Order number given to the customer. UNIQUE so it never repeats. */
    @Column(nullable = false, unique = true, length = 64)
    private String orderNumber;

    @Column(nullable = false)
    private Long userId;

    /** Buy-by-quantity flow. NULL for seat bookings. */
    private Long ticketId;

    /** Seat booking flow. NULL for buy-by-quantity orders. */
    private Long tripId;

    /** The passenger's journey — it determines the price, so it is stored with the order. */
    @Column(length = 8)
    private String fromCode;

    @Column(length = 8)
    private String toCode;

    private int quantity;

    /**
     * Price per ticket AT PURCHASE TIME — later price changes do not affect
     * old orders. NULL for seat bookings: every seat has its own price (lower
     * berth costs more than upper), so no single "unit price" fits the order.
     */
    @Column(precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(precision = 15, scale = 2)
    private BigDecimal totalAmount;

    private int orderStatus;

    /** When payment completed. NULL while unpaid. */
    private LocalDateTime paidAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
