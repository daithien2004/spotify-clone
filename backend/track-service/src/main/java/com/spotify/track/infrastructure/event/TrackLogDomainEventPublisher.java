package com.spotify.track.infrastructure.event;

import com.spotify.track.domain.event.DomainEvent;
import com.spotify.track.domain.repository.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TrackLogDomainEventPublisher implements DomainEventPublisher {
    @Override
    public void publish(DomainEvent event) {
        // Log-only: search-service/notify-service consumers are still Backlog (domain.md event map)
        log.info("Domain Event Published: {} | ID: {} | At: {}",
                event.getClass().getSimpleName(), event.getEventId(), event.getOccurredOn());
    }
}