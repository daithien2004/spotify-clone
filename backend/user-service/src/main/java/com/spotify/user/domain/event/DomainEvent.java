package com.spotify.user.domain.event;

import java.time.LocalDateTime;
import java.util.UUID;

/** Base domain event — idempotent eventId dùng cho de-dupe ở consumer. */
public abstract class DomainEvent {
    private final UUID eventId;
    private final LocalDateTime occurredOn;

    protected DomainEvent() {
        this.eventId = UUID.randomUUID();
        this.occurredOn = LocalDateTime.now();
    }

    public UUID getEventId() {
        return eventId;
    }

    public LocalDateTime getOccurredOn() {
        return occurredOn;
    }
}
