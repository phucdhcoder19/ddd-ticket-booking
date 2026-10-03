package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * One seat or berth on ONE specific trip.
 *
 * This is the real unit of stock for the train business. "997 tickets left"
 * is only a count derived from this table, not the source of truth.
 *
 * Why seats belong to a trip and not to a carriage: seat 12 in carriage 3 may
 * be sold on the trip of the 27th but still free on the 28th. The state
 * differs per day, so there must be one row per (trip, seat).
 *
 * The price is NOT stored here. It depends on how far the passenger travels
 * (Hanoi → Hue differs from Hanoi → Saigon), and the distance is only known
 * when the passenger searches. A stored price would only be right for one
 * station pair.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(
    name = "seat",
    uniqueConstraints = @UniqueConstraint(name = "uk_seat_trip_code", columnNames = {"tripId", "seatCode"}),
    indexes = {
        // The hottest question: "which seats in this class of this trip are free?"
        @Index(name = "idx_seat_trip_class_status", columnList = "tripId,seatClass,status"),
        // Release job: "which seats does this hold own?"
        @Index(name = "idx_seat_hold", columnList = "holdId"),
        // Ticket printing: "which seats belong to this order?"
        @Index(name = "idx_seat_order", columnList = "orderId")
    }
)
public class Seat {

    public static final int STATUS_FREE = 0;
    public static final int STATUS_HELD = 1;
    public static final int STATUS_SOLD = 2;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long tripId;

    /** Code shown to passengers and the frontend: "C3-12" = carriage 3, seat 12. */
    @Column(nullable = false, length = 16)
    private String seatCode;

    private int carriageNumber;

    @Column(nullable = false, length = 16)
    private String seatClass;

    /** Seat number within the carriage, printed on the ticket: "12". */
    @Column(nullable = false, length = 8)
    private String label;

    private int rowNo;
    private int colNo;

    /** Sleeper carriage: compartment number (1..n). Seating carriage: 0. */
    private int compartment;

    /** Berth level: 1 (lowest, most expensive) .. 3. Seating carriage: 0. */
    private int berthLevel;

    @Column(nullable = false)
    private int status;

    /** The hold currently occupying this seat. NULL when the seat is free or sold. */
    private Long holdId;

    /**
     * The order that bought this seat. NULL while unsold.
     *
     * Why the seat must remember its order and not just "sold": printing a
     * ticket needs to know which order the seat belongs to, and a refund must
     * return exactly that seat to stock. status = 2 alone says the seat is sold
     * but not to whom.
     */
    private Long orderId;

    public boolean isFree() {
        return status == STATUS_FREE;
    }

    /** Status name exactly as the frontend SeatStatus type expects. */
    public String statusName() {
        return switch (status) {
            case STATUS_HELD -> "held";
            case STATUS_SOLD -> "sold";
            default -> "available";
        };
    }
}
