package com.spotify.track.application.usecase;

import java.util.List;
import java.util.UUID;

import com.spotify.track.application.dto.TrackResponse;

/** Batch metadata lookup for a list of ids — response preserves the requested order. */
public interface GetTrackByIdsUseCase {
    List<TrackResponse> execute(List<UUID> trackIds);
}