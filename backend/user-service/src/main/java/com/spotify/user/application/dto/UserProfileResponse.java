package com.spotify.user.application.dto;

import com.spotify.user.domain.entity.User;

import java.util.UUID;

/** Public profile của user — không chứa email (tránh lộ PII qua API công khai). */
public record UserProfileResponse(
        UUID id,
        String displayName,
        String avatarUrl,
        long followersCount,
        long followingCount
) {
    public static UserProfileResponse from(User user, long followers, long following) {
        return new UserProfileResponse(user.getId(), user.getDisplayName(), user.getAvatarUrl(),
                followers, following);
    }
}
