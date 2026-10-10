package com.hoangphuc.ddd.controller.support;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hoangphuc.ddd.application.service.auth.AuthAppService;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.cors.CorsUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Gatekeeper for the endpoints that need a signed-in user (paths in WebConfig).
 *
 * An interceptor rather than Spring Security: the rule is one sentence —
 * "these paths need a valid Bearer token" — and the full filter chain would
 * add far more configuration than it saves here.
 *
 * Answers 401 with the usual JSON envelope, so the frontend reads it like any
 * other error and sends the user to the sign-in page.
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private static final String BEARER = "Bearer ";

    private final AuthAppService authAppService;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // The browser's CORS preflight never carries the Authorization header;
        // rejecting it would block the real request behind it.
        if (CorsUtils.isPreFlightRequest(request)) {
            return true;
        }

        String header = request.getHeader("Authorization");
        Optional<Long> userId = header != null && header.startsWith(BEARER)
                ? authAppService.authenticate(header.substring(BEARER.length()).trim())
                : Optional.empty();

        if (userId.isPresent()) {
            request.setAttribute(CurrentUser.ATTRIBUTE, userId.get());
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(
                ResultUtil.error(401, "Please sign in to continue")));
        return false;
    }
}
