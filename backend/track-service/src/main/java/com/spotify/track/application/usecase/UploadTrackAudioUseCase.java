package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.UploadTrackAudioCommand;

public interface UploadTrackAudioUseCase {
    long MAX_AUDIO_BYTES = 50L * 1024 * 1024;

    void execute(UploadTrackAudioCommand command);
}