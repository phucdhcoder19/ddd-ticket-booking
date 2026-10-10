package com.hoangphuc.ddd.infrastructure.security;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * BCrypt, not SHA-256: a password hash must be SLOW. SHA-256 runs billions of
 * times per second on a GPU, so a leaked table of SHA-256 hashes falls to
 * brute force in hours. BCrypt at strength 10 costs ~70ms per guess, and the
 * salt is built into every hash, so equal passwords give different hashes.
 *
 * The only place that imports Spring Security — everything else sees two methods.
 */
@Component
public class PasswordHasher {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(10);

    /**
     * A real hash of a random string. Checked against when the phone number
     * does not exist, so "unknown phone" takes as long as "wrong password" and
     * response time does not reveal which phone numbers have accounts.
     */
    private final String dummyHash = encoder.encode("dummy-password-for-timing");

    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public boolean matches(String rawPassword, String hash) {
        return encoder.matches(rawPassword, hash);
    }

    /** Burn the same time as a real check, always false. */
    public boolean matchesNothing(String rawPassword) {
        encoder.matches(rawPassword, dummyHash);
        return false;
    }
}
