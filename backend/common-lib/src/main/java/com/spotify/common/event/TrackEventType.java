package com.spotify.common.event;

/** Event types carried on the shared track-events topic (domain.md event map). */
public enum TrackEventType {
    TRACK_UPLOADED,
    TRACK_UPDATED,
    TRACK_REMOVED,
    TRACK_AUDIO_UPLOADED
}