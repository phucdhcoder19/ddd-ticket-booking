package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * A carriage of a train service — also a TEMPLATE.
 *
 * "SE1 has 15 carriages: 1-5 soft seats, 6-10 six-berth, 11-15 four-berth."
 * That description holds for every day, so it is stored once.
 *
 * Real seats (the seat table) belong to each trip, because a seat has STATE —
 * seat 12 in carriage 3 may be sold on the 27th but still free on the 28th.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(
    name = "train_carriage",
    indexes = @Index(name = "idx_carriage_train", columnList = "trainId,number")
)
public class TrainCarriage {

    public static final String CLASS_SOFT_SEAT = "SOFT_SEAT";
    public static final String CLASS_BERTH_4   = "BERTH_4";
    public static final String CLASS_BERTH_6   = "BERTH_6";

    public static final String LAYOUT_SEAT_2_2 = "seat-2-2";
    public static final String LAYOUT_BERTH_4  = "berth-4";
    public static final String LAYOUT_BERTH_6  = "berth-6";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long trainId;

    /** Carriage number — passengers read this on the ticket. */
    private int number;

    /** SOFT_SEAT | BERTH_4 | BERTH_6 — matches the frontend SeatClassCode exactly. */
    @Column(nullable = false, length = 16)
    private String seatClass;

    /** seat-2-2 | berth-4 | berth-6 — the frontend uses it to draw the seat map. */
    @Column(nullable = false, length = 16)
    private String layout;

    /**
     * Seating carriage: number of seat rows (4 seats per row, 2-2).
     * Sleeper carriage: number of compartments.
     *
     * The field is rowCount, not rows: "ROWS" is a RESERVED WORD in MySQL 8
     * (used by window functions). A column named rows makes Hibernate generate
     * a CREATE TABLE that MySQL rejects, and the error only shows up at startup,
     * not at compile time.
     */
    private int rowCount;

    /**
     * BUSINESS RULE: how many places a carriage has.
     * Derived from the layout instead of stored — a stored value could drift from rowCount.
     */
    public int seatCount() {
        return switch (layout) {
            case LAYOUT_SEAT_2_2 -> rowCount * 4;   // 2 seats on each side of the aisle
            case LAYOUT_BERTH_4  -> rowCount * 4;   // 4 berths per compartment
            case LAYOUT_BERTH_6  -> rowCount * 6;   // 6 berths per compartment
            default -> 0;
        };
    }

    /** Number of berth levels. Seating carriages return 0. */
    public int berthLevels() {
        return switch (layout) {
            case LAYOUT_BERTH_4 -> 2;
            case LAYOUT_BERTH_6 -> 3;
            default -> 0;
        };
    }
}
