package com.hoangphuc.ddd.application.service.idempotency;

/**
 * Idempotency-Key handling: "the same action sent twice is performed once".
 *
 * Typical flow for one request:
 *
 *   begin()  ── FRESH ──────► run the action ─┬─ success ─► complete(response)
 *            │                                └─ failure ─► abandon()
 *            ├─ REPLAY ─────► return the stored response, do NOT run the action
 *            ├─ KEY_REUSED ─► the key was used for a DIFFERENT request -> reject
 *            └─ IN_PROGRESS ► the first request is still running -> ask to retry later
 */
public interface IdempotencyAppService {

    enum Outcome { FRESH, REPLAY, KEY_REUSED, IN_PROGRESS }

    /** storedResponse is only set for REPLAY. */
    record Claim(Outcome outcome, String storedResponse) {
    }

    /**
     * @param scope       which action ("holds", "orders") — the same key on two endpoints are two keys
     * @param userId      keys are per user, so one user can never replay another user's response
     * @param key         the Idempotency-Key header
     * @param fingerprint hash of the request body, to detect a key reused for a different request
     */
    Claim begin(String scope, Long userId, String key, String fingerprint);

    /** Store the successful response so retries get exactly the same answer. */
    void complete(String scope, Long userId, String key, String fingerprint, String response);

    /** The action failed: free the key so a retry runs the action for real. */
    void abandon(String scope, Long userId, String key);
}
