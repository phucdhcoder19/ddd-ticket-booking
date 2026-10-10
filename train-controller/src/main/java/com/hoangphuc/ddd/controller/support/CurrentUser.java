package com.hoangphuc.ddd.controller.support;

import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Who is calling: the user id AuthInterceptor took from the verified token.
 *
 * It is read here and NEVER from the request body: accepting any userId from
 * the client would let anyone book on behalf of others and read other
 * people's tickets through GET /me/tickets.
 */
public final class CurrentUser {

    /** Request attribute set by AuthInterceptor. */
    static final String ATTRIBUTE = CurrentUser.class.getName() + ".id";

    private CurrentUser() {
    }

    /**
     * @throws IllegalStateException when nobody is signed in. That is a bug, not
     *         a user error: the endpoint calling this is missing from the paths
     *         AuthInterceptor protects (WebConfig.addInterceptors).
     */
    public static Long id() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        Object id = attributes == null ? null : attributes.getAttribute(ATTRIBUTE, RequestAttributes.SCOPE_REQUEST);
        if (id instanceof Long userId) {
            return userId;
        }
        throw new IllegalStateException("No signed-in user: add this endpoint to AuthInterceptor's paths in WebConfig");
    }
}
