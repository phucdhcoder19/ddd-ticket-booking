package com.hoangphuc.ddd.application.service.auth;

import com.hoangphuc.ddd.application.model.AuthResult;
import com.hoangphuc.ddd.application.model.UserDTO;

import java.util.Optional;

public interface AuthAppService {

    /** Create an account and sign it in. The phone must already be normalised to 0xxxxxxxxx. */
    AuthResult register(String phone, String fullName, String password);

    AuthResult login(String phone, String password);

    /** The user id behind a token, or empty if the token is missing, forged or expired. */
    Optional<Long> authenticate(String token);

    Optional<UserDTO> getUser(Long userId);
}
