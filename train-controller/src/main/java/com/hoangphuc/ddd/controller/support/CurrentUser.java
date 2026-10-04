package com.hoangphuc.ddd.controller.support;

/**
 * Who is calling. No login yet, so every request is the same demo user.
 *
 * It is read here and NEVER from the request body: accepting any userId from
 * the client would let anyone book on behalf of others and, worse, read other
 * people's tickets through GET /me/tickets. When login is plugged in, this is
 * the one place to change — return the id from the authenticated principal.
 */
public final class CurrentUser {

    private static final Long DEMO_USER_ID = 1L;

    private CurrentUser() {
    }

    public static Long id() {
        return DEMO_USER_ID;
    }
}
