package com.hoangphuc.ddd.domain.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * A customer account. Signs in with a phone number: in Vietnam that is what
 * people book trains with, and it is already collected for every passenger.
 *
 * Table "app_user", not "user": USER is a reserved word in MySQL.
 */
@Data
@Accessors(chain = true)
@Entity
@Table(
    name = "app_user",
    uniqueConstraints = @UniqueConstraint(name = "uk_app_user_phone", columnNames = "phone")
)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Normalised to 0xxxxxxxxx, the same format as passenger phones. */
    @Column(nullable = false, length = 10)
    private String phone;

    @Column(nullable = false, length = 100)
    private String fullName;

    /** BCrypt hash. The password itself is never stored or logged. */
    @Column(nullable = false, length = 60)
    private String passwordHash;

    private LocalDateTime createdAt;
}
