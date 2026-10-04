package com.hoangphuc.ddd.controller.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoangphuc.ddd.application.service.idempotency.IdempotencyAppService;
import com.hoangphuc.ddd.application.service.idempotency.IdempotencyAppService.Claim;
import com.hoangphuc.ddd.controller.model.vo.ResultMessage;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Wraps a controller action so that requests carrying the same Idempotency-Key
 * are performed once and answered identically.
 *
 * Lives in the controller layer because it works on ResultMessage — the exact
 * HTTP response — and on a header. The bookkeeping itself (Redis, waiting,
 * fingerprint check) is in IdempotencyAppService.
 *
 * The header is OPTIONAL: without it the action simply runs. Scripts, JMeter
 * and older clients keep working, while the web app always sends one.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class IdempotentRequests {

    public static final String HEADER = "Idempotency-Key";

    /** UUIDs and the frontend's fallback "k_<time>_<random>" both fit. Anything else is rejected rather than stored. */
    private static final Pattern VALID_KEY = Pattern.compile("^[A-Za-z0-9_-]{8,100}$");

    private final IdempotencyAppService idempotencyAppService;

    /** Spring's own ObjectMapper: stored responses are serialized exactly like real HTTP responses. */
    private final ObjectMapper objectMapper;

    public <T> ResultMessage<T> run(String scope, String key, Object request, Class<T> resultType,
                                    Supplier<ResultMessage<T>> action) {
        if (key == null || key.isBlank()) {
            return action.get();
        }
        if (!VALID_KEY.matcher(key).matches()) {
            return ResultUtil.error(400, "Invalid Idempotency-Key: use 8-100 letters, digits, '-' or '_'");
        }

        Long userId = CurrentUser.id();
        String fingerprint = fingerprint(request);
        Claim claim = idempotencyAppService.begin(scope, userId, key, fingerprint);

        switch (claim.outcome()) {
            case REPLAY:
                return replay(claim.storedResponse(), resultType);
            case KEY_REUSED:
                // 422: the request is well-formed but contradicts what this key already means
                return ResultUtil.error(422, "This Idempotency-Key was already used for a different request");
            case IN_PROGRESS:
                // 429 so the client treats it as "busy, try again shortly"
                return ResultUtil.error(429, "The same request is still being processed, please wait a moment");
            case FRESH:
            default:
                break;
        }

        ResultMessage<T> response;
        try {
            response = action.get();
        } catch (RuntimeException e) {
            idempotencyAppService.abandon(scope, userId, key);
            throw e;
        }

        if (response.isSuccess()) {
            try {
                idempotencyAppService.complete(scope, userId, key, fingerprint, objectMapper.writeValueAsString(response));
            } catch (JsonProcessingException | RuntimeException e) {
                // The action DID succeed; failing to record it must not turn the
                // answer into an error. Worst case a retry runs the action again
                // and gets the business answer (e.g. "seat taken").
                log.warn("[IDEM] could not store the response | scope={} key={}", scope, key, e);
                idempotencyAppService.abandon(scope, userId, key);
            }
        } else {
            idempotencyAppService.abandon(scope, userId, key);
        }
        return response;
    }

    private <T> ResultMessage<T> replay(String json, Class<T> resultType) {
        JavaType type = objectMapper.getTypeFactory().constructParametricType(ResultMessage.class, resultType);
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            log.error("[IDEM] stored response is unreadable", e);
            return ResultUtil.error(500, "System error, please try again");
        }
    }

    /** SHA-256 of the request body as JSON: same body -> same fingerprint, any changed field -> different. */
    private String fingerprint(Object request) {
        try {
            byte[] json = objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(json));
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new IllegalStateException("Cannot fingerprint the request", e);
        }
    }
}
