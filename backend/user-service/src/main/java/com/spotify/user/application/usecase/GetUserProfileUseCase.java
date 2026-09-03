package com.spotify.user.application.usecase;

import com.spotify.user.application.dto.UserProfileResponse;

import java.util.UUID;

public interface GetUserProfileUseCase {
    UserProfileResponse execute(UUID id);
}
