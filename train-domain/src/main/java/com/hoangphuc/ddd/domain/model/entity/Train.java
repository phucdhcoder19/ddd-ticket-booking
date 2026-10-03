package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * A train service — a TEMPLATE, not tied to any date.
 *
 * SE1 departs at 20:25 every day. "SE1" itself is not a specific journey;
 * it is a schedule. A specific journey is a Trip = Train + date.
 *
 * Separating the template from each run avoids duplicated data: changing
 * SE1's departure time edits one row, not 60 rows for 60 days.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(name = "train")
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** SE1, SE3, TN3... */
    @Column(nullable = false, unique = true, length = 16)
    private String code;

    private int departHour;
    private int departMinute;

    /** Average speed in km/h — used to estimate the arrival time. */
    private int speedKmh;

    /** 0 = out of service, 1 = running */
    private int status;
}
