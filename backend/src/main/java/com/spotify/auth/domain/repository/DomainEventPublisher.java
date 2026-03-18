package com.spotify.auth.domain.repository;

import com.spotify.auth.domain.event.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
