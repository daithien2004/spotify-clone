package com.spotify.track.domain.repository;

import com.spotify.track.domain.event.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}