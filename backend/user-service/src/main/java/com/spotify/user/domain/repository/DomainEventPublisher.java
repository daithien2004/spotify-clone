package com.spotify.user.domain.repository;

import com.spotify.user.domain.event.DomainEvent;

public interface DomainEventPublisher {
    void publish(DomainEvent event);
}
