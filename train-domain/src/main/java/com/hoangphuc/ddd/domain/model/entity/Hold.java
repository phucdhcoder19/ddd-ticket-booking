package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * TIME-LIMITED SEAT HOLD — the core of lesson 18.
 *
 * A hold means "someone is looking at these seats, do not sell them to anyone
 * else": temporary, with a deadline, gone once time runs out. Very different
 * from TicketOrder — an order is a payment record that lives forever and is
 * never deleted automatically.
 *
 * They are separate because their lifecycles differ. Put both in one table
 * and you get a pile of PENDING rows that mean both "browsing" and "bought
 * but not paid" — impossible to tell apart, and revenue reports have to
 * filter out the noise.
 *
 * ──────────────────────────────────────────────────────────────────────────
 * HOLD SPECIFIC SEATS, NOT "ANY N SEATS"
 *
 * The first version of this table held by quantity: ticketId + quantity, with
 * stock as a single number. That model fits event tickets, not train tickets —
 * a passenger picks the exact lower berth in compartment 3 of carriage 11,
 * not "some berth".
 *
 * So Hold has NO quantity column anymore. The seat list lives on the seat
 * table (seat.hold_id points here), because the seat is the thing whose state
 * must be locked. To see which seats a hold owns:
 * SELECT * FROM seat WHERE hold_id = ?
 *
 * seatCount and totalAmount are FIXED at hold time and kept here so we do not
 * have to recount and reprice every time the client polls the countdown
 * (every 5 seconds).
 */
@Data
@Accessors(chain = true)
@Entity
@Table(
    name = "ticket_hold",
    indexes = {
        @Index(name = "uk_hold_code", columnList = "holdCode", unique = true),
        // The expiry job runs WHERE status = 0 AND expire_at < now.
        // Composite index in that order: filter on the first column, range on the second.
        @Index(name = "idx_hold_status_expire", columnList = "status,expireAt")
    }
)
public class Hold {

    public static final int STATUS_HOLDING  = 0;   // active, not expired yet
    public static final int STATUS_USED     = 1;   // converted into an order
    public static final int STATUS_RELEASED = 2;   // seats returned (user cancelled or time ran out)

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Public code returned to the client. The auto-increment id is never
     * exposed — knowing id=41 lets you guess someone else's id=42.
     */
    @Column(nullable = false, unique = true, length = 40)
    private String holdCode;

    @Column(nullable = false)
    private Long userId;

    /** The trip being held. A seat only has meaning within one trip. */
    @Column(nullable = false)
    private Long tripId;

    /**
     * The PASSENGER's journey, not the train's.
     *
     * SE1 runs Hanoi – Saigon, but a passenger may only ride Hue – Da Nang, and
     * the fare is based on that segment. Without the station pair on the hold
     * there is no way to recompute the right amount when the order is created.
     */
    @Column(nullable = false, length = 8)
    private String fromCode;

    @Column(nullable = false, length = 8)
    private String toCode;

    /** Number of seats held — fixed at hold time so polling does not need a COUNT. */
    private int seatCount;

    /** Provisional total, before each passenger's discount. */
    private long totalAmount;

    @Column(nullable = false)
    private int status;

    /** When the hold expires. This is the SOURCE OF TRUTH; the client only counts down to it. */
    @Column(nullable = false)
    private LocalDateTime expireAt;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /**
     * BUSINESS RULE: is the hold still active?
     * Takes "now" as a parameter — same reason as TicketDetail.isOpenedForSale():
     * tests can pass any instant, and Jackson does not treat it as a field.
     */
    public boolean isHolding(LocalDateTime now) {
        return status == STATUS_HOLDING && now.isBefore(expireAt);
    }

    /** Seconds left, never negative — the client uses it to draw the timer. */
    public long secondsLeft(LocalDateTime now) {
        if (status != STATUS_HOLDING) return 0;
        long s = Duration.between(now, expireAt).toSeconds();
        return Math.max(s, 0);
    }
}
