package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * One specific run: which train, which date.
 *
 * LAZILY PROVISIONED: a trip is only created when the first person searches
 * for that date.
 *
 * Why not pre-generate 60 days: each trip brings ~600 seats.
 *   7 trains × 60 days × 600 seats = 252,000 rows
 * while most days nobody searches. Lazy provisioning only pays for days that
 * actually have customers.
 *
 * The trade-off: the first searcher waits longer, and when 5,000 people search
 * at once we must stop 5,000 provisioning runs from starting — see
 * TripProvisionService.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(
    name = "trip",
    uniqueConstraints = @UniqueConstraint(name = "uk_trip_train_date", columnNames = {"trainId", "serviceDate"}),
    indexes = @Index(name = "idx_trip_date", columnList = "serviceDate")
)
public class Trip {

    public static final int STATUS_READY = 1;   // all seats generated, ready to sell

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long trainId;

    /** Service date (the departure time comes from Train). */
    @Column(nullable = false)
    private LocalDate serviceDate;

    @Column(nullable = false)
    private int status;

    private LocalDateTime createdAt;
}
