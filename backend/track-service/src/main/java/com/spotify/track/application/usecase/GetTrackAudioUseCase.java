package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.GetTrackAudioCommand;
import com.spotify.track.domain.entity.TrackAudioRange;

public interface GetTrackAudioUseCase {
    TrackAudioRange execute(GetTrackAudioCommand command);
}