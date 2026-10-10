package com.hoangphuc.ddd.controller.http;

import com.hoangphuc.ddd.application.model.AuthResult;
import com.hoangphuc.ddd.application.model.AuthSessionDTO;
import com.hoangphuc.ddd.application.service.auth.AuthAppService;
import com.hoangphuc.ddd.controller.dto.LoginRequest;
import com.hoangphuc.ddd.controller.dto.RegisterRequest;
import com.hoangphuc.ddd.controller.model.vo.ResultMessage;
import com.hoangphuc.ddd.controller.model.vo.ResultUtil;
import com.hoangphuc.ddd.controller.support.Phones;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.regex.Pattern;

/**
 *   POST /auth/register   create an account, answer with a token (signed in right away)
 *   POST /auth/login      phone + password -> token
 *
 * There is no /auth/logout: a JWT lives on the client, so signing out means
 * the client forgets it. The server cannot revoke it before it expires.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    /** Same rule as passenger phones on the frontend (validatePhone): 10 digits starting with 0. */
    private static final Pattern PHONE = Pattern.compile("^0\\d{9}$");

    private final AuthAppService authAppService;

    @PostMapping("/register")
    public ResultMessage<AuthSessionDTO> register(@Valid @RequestBody RegisterRequest request) {
        String phone = Phones.normalize(request.getPhone());
        if (!PHONE.matcher(phone).matches()) {
            return ResultUtil.error(400, "The phone number must have 10 digits and start with 0");
        }
        String fullName = request.getFullName().trim().replaceAll("\\s+", " ");
        return toResponse(authAppService.register(phone, fullName, request.getPassword()));
    }

    @PostMapping("/login")
    public ResultMessage<AuthSessionDTO> login(@Valid @RequestBody LoginRequest request) {
        return toResponse(authAppService.login(Phones.normalize(request.getPhone()), request.getPassword()));
    }

    private ResultMessage<AuthSessionDTO> toResponse(AuthResult result) {
        return switch (result.getStatus()) {
            case SUCCESS           -> ResultUtil.data(result.getSession());
            // 422, not 409: the frontend reads 409 as "sold out"
            case PHONE_TAKEN       -> ResultUtil.error(422, "This phone number already has an account, please sign in");
            // 401 would make the frontend think a valid session just expired
            case WRONG_CREDENTIALS -> ResultUtil.error(422, "Wrong phone number or password");
            case TOO_MANY_ATTEMPTS -> ResultUtil.error(429, "Too many failed attempts, please wait a few minutes and try again");
        };
    }
}
