package com.spotify.search.application.dto;

import com.spotify.search.domain.entity.TrackSearchDocument;
import java.util.UUID;

/** Search result item returned to the gateway/FE (spec §4). */
public record SearchTrackItem(
        UUID id,
        String title,
        String artist,
        String album,
        String artworkUrl,
        String audioUrl,
        Long durationMs
) {
    public static SearchTrackItem from(TrackSearchDocument doc) {
        return new SearchTrackItem(doc.id(), doc.title(), doc.artist(), doc.album(),
                doc.artworkUrl(), doc.audioUrl(), doc.durationMs());
    }
}
