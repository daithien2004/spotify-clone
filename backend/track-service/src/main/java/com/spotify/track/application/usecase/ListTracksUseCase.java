package com.spotify.track.application.usecase;

import com.spotify.track.application.dto.TrackResponse;
import java.util.List;

/** Full-catalog read used by the search-service startup bootstrap. */
public interface ListTracksUseCase {
    List<TrackResponse> execute();
}
