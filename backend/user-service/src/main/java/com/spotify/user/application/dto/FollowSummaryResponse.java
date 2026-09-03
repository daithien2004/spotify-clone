package com.spotify.user.application.dto;

import com.spotify.user.domain.entity.Follow;
import com.spotify.user.domain.entity.User;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Một dòng trong danh sách followers/following: profile user + thời điểm follow. */
public record FollowSummaryResponse(
        UUID id,
        String displayName,
        String avatarUrl,
        OffsetDateTime followedAt
) {
    public static FollowSummaryResponse of(User user, Follow follow) {
        return new FollowSummaryResponse(user.getId(), user.getDisplayName(),
                user.getAvatarUrl(), follow.getCreatedAt());
    }
}
