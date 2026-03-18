package com.spotify.auth.domain.repository;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.valueobject.Email;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(Email email);
    boolean existsByEmail(Email email);
}
