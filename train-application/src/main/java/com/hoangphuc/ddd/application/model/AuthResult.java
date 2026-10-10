package com.hoangphuc.ddd.application.model;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AuthResult {

    public enum Status {
        SUCCESS,
        PHONE_TAKEN,
        /** Unknown phone OR wrong password — deliberately one status, see AuthAppServiceImpl.login(). */
        WRONG_CREDENTIALS,
        TOO_MANY_ATTEMPTS
    }

    private final Status status;
    private final AuthSessionDTO session;

    public static AuthResult success(AuthSessionDTO session) {
        return new AuthResult(Status.SUCCESS, session);
    }

    public static AuthResult fail(Status status) {
        return new AuthResult(status, null);
    }
}
