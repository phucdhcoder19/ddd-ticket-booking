package com.hoangphuc.ddd.application.model;

import lombok.Data;

import java.time.Instant;

/** Answer to a successful sign-in or registration. */
@Data
public class AuthSessionDTO {

    /** Sent back as "Authorization: Bearer <token>" on every request. */
    private String token;

    /** The client signs the user out at this moment instead of waiting for a 401. */
    private Instant expiresAt;

    private UserDTO user;
}
