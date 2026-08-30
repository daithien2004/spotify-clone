package com.spotify.search.infrastructure.bootstrap;

import java.util.List;

import com.spotify.common.event.TrackEventEnvelope.TrackPayload;

/** Pulls the historical catalog once at startup (spec §6.1). */
public interface TrackBootstrapFetcher {
    List<TrackPayload> fetchAll();
}