package com.spotify.user.domain.repository;

import com.spotify.user.domain.entity.User;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findById(UUID id);

    Optional<User> findByEmail(String email);

    /** Batch lookup cho followers/following lists — giữ thứ tự input. */
    List<User> findByIds(List<UUID> ids);

    User save(User user);
}
