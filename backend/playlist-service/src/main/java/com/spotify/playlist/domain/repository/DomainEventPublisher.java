package com.spotify.playlist.domain.repository;

import com.spotify.playlist.domain.event.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
