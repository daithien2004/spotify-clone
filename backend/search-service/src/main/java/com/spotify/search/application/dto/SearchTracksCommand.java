package com.spotify.search.application.dto;

/** Input for a full-text query — blank q is rejected by the use case. */
public record SearchTracksCommand(String query, int limit) {}
