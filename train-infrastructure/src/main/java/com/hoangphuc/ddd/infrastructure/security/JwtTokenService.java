package com.hoangphuc.ddd.infrastructure.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;

/**
 * JWT (HS256) — header.payload.signature, each part base64url.
 *
 * Why a token instead of a server-side session: any instance can check it
 * with nothing but the secret, no shared session store and no DB lookup per
 * request. The price: a token cannot be revoked before it expires, so the
 * lifetime is kept short.
 *
 * Written by hand (~60 lines) instead of pulling a JWT library, to show what a
 * JWT really is. The three checks that matter, in verify():
 *   1. the header must be EXACTLY ours — rejects "alg":"none" and algorithm swaps
 *   2. the signature is compared in constant time (MessageDigest.isEqual)
 *   3. "exp" must be in the future
 * The payload is only base64, NOT encrypted: anyone can read it, so it holds
 * the user id and nothing secret.
 */
@Component
public class JwtTokenService {

    private static final Base64.Encoder B64 = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder B64_DECODE = Base64.getUrlDecoder();
    private static final String ALGORITHM = "HmacSHA256";
    private static final String HEADER = B64.encodeToString(
            "{\"alg\":\"HS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final SecretKeySpec key;
    private final Duration ttl;

    public JwtTokenService(@Value("${app.auth.jwt-secret}") String secret,
                           @Value("${app.auth.token-ttl:24h}") Duration ttl) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        // HS256 with a short secret can be brute-forced offline from any one token
        if (bytes.length < 32) {
            throw new IllegalStateException("app.auth.jwt-secret must be at least 32 bytes");
        }
        this.key = new SecretKeySpec(bytes, ALGORITHM);
        this.ttl = ttl;
    }

    public record IssuedToken(String token, Instant expiresAt) {
    }

    public IssuedToken issue(Long userId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(ttl);
        String payload = encodeJson(Map.of(
                "sub", String.valueOf(userId),
                "iat", now.getEpochSecond(),
                "exp", expiresAt.getEpochSecond()));
        String unsigned = HEADER + "." + payload;
        return new IssuedToken(unsigned + "." + B64.encodeToString(sign(unsigned)), expiresAt);
    }

    /** @return the user id, or empty for anything malformed, forged or expired */
    public Optional<Long> verify(String token) {
        if (token == null) {
            return Optional.empty();
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3 || !HEADER.equals(parts[0])) {
            return Optional.empty();
        }
        try {
            byte[] expected = sign(parts[0] + "." + parts[1]);
            if (!MessageDigest.isEqual(expected, B64_DECODE.decode(parts[2]))) {
                return Optional.empty();
            }
            JsonNode claims = objectMapper.readTree(B64_DECODE.decode(parts[1]));
            if (claims.path("exp").asLong(0) <= Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(Long.parseLong(claims.path("sub").asText()));
        } catch (Exception e) {
            // Bad base64, bad JSON, bad number: all mean "not a token we issued"
            return Optional.empty();
        }
    }

    /** Mac is not thread-safe, so one per call — cheap next to a network round trip. */
    private byte[] sign(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(key);
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 unavailable", e);
        }
    }

    private String encodeJson(Map<String, Object> claims) {
        try {
            return B64.encodeToString(objectMapper.writeValueAsBytes(claims));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot encode JWT claims", e);
        }
    }
}
