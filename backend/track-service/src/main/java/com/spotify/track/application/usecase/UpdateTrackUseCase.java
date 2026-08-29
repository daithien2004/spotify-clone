package com.spotify.track.application.usecase;

import java.util.UUID;

import com.spotify.track.application.dto.CreateTrackRequest;
import com.spotify.track.application.dto.TrackResponse;

public interface UpdateTrackUseCase {
    TrackResponse execute(UUID trackId, CreateTrackRequest request);
}