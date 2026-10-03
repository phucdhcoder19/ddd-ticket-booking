package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * The person who will travel on ONE seat of a hold.
 *
 * Train tickets are named tickets: the conductor checks the name and ID
 * number printed on the ticket against a real ID document. So every seat
 * must have exactly one passenger before it can become an order.
 *
 * Why a separate table instead of extra columns on seat: seat is the trip's
 * STOCK — its rows live forever and are sold again when a hold expires. With
 * a name written on the seat, an expired hold would have to remember to wipe
 * it, and forgetting means the next buyer sees the previous person's name.
 * In a separate table, rows of an expired hold are simply never used again.
 *
 * THE PRICE IS FIXED HERE. basePrice is the seat fare for the journey,
 * finalPrice is after this person's discount. The order adds up finalPrice
 * without recomputing it, for the same reason as totalAmount on Hold.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(
    name = "hold_passenger",
    uniqueConstraints = @UniqueConstraint(name = "uk_hold_passenger_seat", columnNames = {"holdId", "seatCode"}),
    indexes = {
        // "My tickets" and ticket printing: which passengers belong to this order.
        @Index(name = "idx_hold_passenger_order", columnList = "orderId")
    }
)
public class HoldPassenger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long holdId;

    /** Seat code within the trip: "C3-12". Same code as seat.seatCode. */
    @Column(nullable = false, length = 16)
    private String seatCode;

    @Column(nullable = false, length = 100)
    private String fullName;

    /** 12-digit national ID, or a 9-digit legacy ID card. */
    @Column(nullable = false, length = 12)
    private String idNumber;

    /** Normalised to the 0xxxxxxxxx format. */
    @Column(nullable = false, length = 10)
    private String phone;

    /** Name of the PassengerDiscount. Stored as text, not ordinal: inserting a value into the middle of the enum would break every old row. */
    @Column(nullable = false, length = 16)
    private String discount;

    private long basePrice;
    private long finalPrice;

    /** The order that bought this ticket. NULL until the hold becomes an order. */
    private Long orderId;

    private LocalDateTime createdAt;
}
