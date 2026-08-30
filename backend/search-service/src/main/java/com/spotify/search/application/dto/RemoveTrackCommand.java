package com.spotify.search.application.dto;

import java.util.UUID;

/** Input for removing one track from the index. */
public record RemoveTrackCommand(UUID trackId) {}
