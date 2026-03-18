package com.spotify.auth.infrastructure.event;

import com.spotify.auth.domain.event.DomainEvent;
import com.spotify.auth.domain.repository.DomainEventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogDomainEventPublisher implements DomainEventPublisher {
    @Override
    public void publish(DomainEvent event) {
        log.info("Domain Event Published: {} | ID: {} | At: {}", 
            event.getClass().getSimpleName(), event.getEventId(), event.getOccurredOn());
    }
}
