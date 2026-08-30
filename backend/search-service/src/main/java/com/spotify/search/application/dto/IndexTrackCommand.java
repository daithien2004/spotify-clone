package com.spotify.search.application.dto;

import com.spotify.search.domain.entity.TrackSearchDocument;

/** Input for indexing one track (from a Kafka event or the bootstrap). */
public record IndexTrackCommand(TrackSearchDocument document) {}
