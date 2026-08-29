package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.CreateTrackRequest;
import com.spotify.track.domain.entity.Track;

public interface CreateTrackUseCase {
    Track execute(CreateTrackRequest request);
}