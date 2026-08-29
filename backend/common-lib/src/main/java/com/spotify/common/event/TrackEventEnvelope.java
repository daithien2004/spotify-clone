package com.spotify.common.event;

/**
 * Cross-service Kafka contract (track-service producer → search-service consumer), topic
 * {@code spotify.track.events}. Plain record — serialized with Jackson's record support;
 * {@code occurredOn} is ISO-8601 text so both sides need no JSR-310 module tweaks.
 */
public record TrackEventEnvelope(
        TrackEventType eventType,
        String eventId,
        String occurredOn,
        TrackPayload track
) {
    /** Snapshot of the track aggregate carried by the event. */
    public record TrackPayload(
            String id,
            String title,
            String artist,
            String album,
            Long durationMs,
            String artworkUrl,
            String audioUrl
    ) {}
}