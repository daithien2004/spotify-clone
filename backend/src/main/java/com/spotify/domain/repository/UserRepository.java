package com.spotify.domain.repository;

import com.spotify.domain.entity.User;
import com.spotify.domain.valueobject.Email;

import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(Email email);
    boolean existsByEmail(Email email);
}
