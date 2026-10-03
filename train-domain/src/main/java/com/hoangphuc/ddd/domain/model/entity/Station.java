package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * A station on the North–South line.
 *
 * Pure lookup table: almost never changes, and there is no business logic
 * beyond "list them for the user to pick". Exactly the kind of data to cache
 * aggressively.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(name = "station", indexes = @Index(name = "idx_station_order", columnList = "displayOrder"))
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Short station code used in URLs and ticket codes: HNO, SGO... */
    @Column(nullable = false, unique = true, length = 8)
    private String code;

    @Column(nullable = false, length = 64)
    private String name;

    /** "North" | "Central" | "South" — groups a long list so it is easier to scan */
    @Column(nullable = false, length = 8)
    private String region;

    /** Kilometres from Hanoi station. Used to estimate distance and fare. */
    private int kmFromHanoi;

    /** Position on the North–South line. Lists are ALWAYS sorted by this column,
     *  not by name — travellers think in route order. */
    private int displayOrder;
}
