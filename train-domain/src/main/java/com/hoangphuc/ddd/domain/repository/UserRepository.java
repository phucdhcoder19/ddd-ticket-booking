package com.hoangphuc.ddd.domain.repository;

import com.hoangphuc.ddd.domain.model.entity.User;

import java.util.Optional;

public interface UserRepository {

    /**
     * @throws org.springframework.dao.DataIntegrityViolationException when the
     *         phone is already taken — the unique key is the real guard, a
     *         findByPhone() check before saving can lose a race
     */
    User save(User user);

    Optional<User> findById(Long id);

    Optional<User> findByPhone(String phone);
}
