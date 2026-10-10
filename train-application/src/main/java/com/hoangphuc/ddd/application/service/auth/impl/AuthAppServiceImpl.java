package com.hoangphuc.ddd.application.service.auth.impl;

import com.hoangphuc.ddd.application.model.AuthResult;
import com.hoangphuc.ddd.application.model.AuthSessionDTO;
import com.hoangphuc.ddd.application.model.UserDTO;
import com.hoangphuc.ddd.application.service.auth.AuthAppService;
import com.hoangphuc.ddd.domain.model.entity.User;
import com.hoangphuc.ddd.domain.repository.UserRepository;
import com.hoangphuc.ddd.infrastructure.cache.redis.LoginAttemptRedisStore;
import com.hoangphuc.ddd.infrastructure.security.JwtTokenService;
import com.hoangphuc.ddd.infrastructure.security.PasswordHasher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthAppServiceImpl implements AuthAppService {

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final JwtTokenService jwtTokenService;
    private final LoginAttemptRedisStore loginAttempts;

    /** Failed sign-ins allowed per phone within the window before it is locked. */
    @Value("${app.auth.max-failed-logins:5}")
    private int maxFailedLogins;

    @Value("${app.auth.lockout-window:15m}")
    private Duration lockoutWindow;

    @Override
    public AuthResult register(String phone, String fullName, String password) {
        User user = new User()
                .setPhone(phone)
                .setFullName(fullName)
                .setPasswordHash(passwordHasher.hash(password))
                .setCreatedAt(LocalDateTime.now());
        try {
            // No findByPhone() check first: two sign-ups with the same phone
            // could both pass it. The unique key decides, exactly once.
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            return AuthResult.fail(AuthResult.Status.PHONE_TAKEN);
        }
        log.info("[AUTH] registered | user={}", user.getId());
        return AuthResult.success(sessionFor(user));
    }

    /**
     * Unknown phone and wrong password give the SAME answer and take the SAME
     * time (matchesNothing() runs a real BCrypt check). Otherwise an attacker
     * learns which phone numbers have accounts, then targets only those.
     */
    @Override
    public AuthResult login(String phone, String password) {
        if (loginAttempts.failures(phone) >= maxFailedLogins) {
            log.warn("[AUTH] locked out | phone={}", mask(phone));
            return AuthResult.fail(AuthResult.Status.TOO_MANY_ATTEMPTS);
        }

        Optional<User> found = userRepository.findByPhone(phone);
        boolean ok = found.isPresent()
                ? passwordHasher.matches(password, found.get().getPasswordHash())
                : passwordHasher.matchesNothing(password);
        if (!ok) {
            long failures = loginAttempts.recordFailure(phone, lockoutWindow);
            log.info("[AUTH] wrong credentials | phone={} failures={}", mask(phone), failures);
            return AuthResult.fail(AuthResult.Status.WRONG_CREDENTIALS);
        }

        loginAttempts.reset(phone);
        log.info("[AUTH] signed in | user={}", found.get().getId());
        return AuthResult.success(sessionFor(found.get()));
    }

    @Override
    public Optional<Long> authenticate(String token) {
        return jwtTokenService.verify(token);
    }

    @Override
    public Optional<UserDTO> getUser(Long userId) {
        return userRepository.findById(userId).map(this::toDTO);
    }

    private AuthSessionDTO sessionFor(User user) {
        JwtTokenService.IssuedToken issued = jwtTokenService.issue(user.getId());
        AuthSessionDTO session = new AuthSessionDTO();
        session.setToken(issued.token());
        session.setExpiresAt(issued.expiresAt());
        session.setUser(toDTO(user));
        return session;
    }

    private UserDTO toDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setPhone(user.getPhone());
        dto.setFullName(user.getFullName());
        return dto;
    }

    /** Phone numbers are personal data: logs keep only enough to correlate. */
    private String mask(String phone) {
        return phone.length() < 7 ? phone : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 3);
    }
}
