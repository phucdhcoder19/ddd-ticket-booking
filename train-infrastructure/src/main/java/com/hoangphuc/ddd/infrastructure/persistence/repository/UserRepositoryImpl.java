package com.hoangphuc.ddd.infrastructure.persistence.repository;

import com.hoangphuc.ddd.domain.model.entity.User;
import com.hoangphuc.ddd.domain.repository.UserRepository;
import com.hoangphuc.ddd.infrastructure.persistence.mapper.UserJPAMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserJPAMapper userJPAMapper;

    @Override
    public User save(User user) {
        // saveAndFlush: hit the unique key NOW, inside this call, so the caller
        // can catch the duplicate instead of it surfacing later at commit.
        return userJPAMapper.saveAndFlush(user);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userJPAMapper.findById(id);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        return userJPAMapper.findByPhone(phone);
    }
}
