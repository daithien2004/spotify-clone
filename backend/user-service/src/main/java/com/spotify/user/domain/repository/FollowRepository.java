package com.spotify.user.domain.repository;

import com.spotify.user.domain.entity.Follow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FollowRepository {
    Optional<Follow> findFollowing(UUID followerId, UUID followeeId);

    /** Who current {@code userId} follows — for following list. */
    List<Follow> findAllByFollower(UUID followerId);

    /** Who follows {@code userId} — for followers list. */
    List<Follow> findAllByFollowee(UUID followeeId);

    boolean exists(UUID followerId, UUID followeeId);

    void save(Follow follow);

    void delete(Follow follow);
}
