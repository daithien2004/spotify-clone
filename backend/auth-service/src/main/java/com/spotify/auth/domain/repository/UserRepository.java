package com.spotify.auth.domain.repository;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.valueobject.Email;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(Email email);
    Optional<User> findById(UUID id);
    boolean existsByEmail(Email email);
}
