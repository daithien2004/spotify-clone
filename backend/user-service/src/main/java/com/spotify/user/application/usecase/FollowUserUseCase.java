package com.spotify.user.application.usecase;

import com.spotify.user.domain.entity.Follow;

import java.util.UUID;

public interface FollowUserUseCase {
    /** follower theo dõi followee; idempotent (trả follow hiện có nếu đã follow). */
    Follow execute(UUID followerId, UUID followeeId);
}
