package com.spotify.user.application.usecase;

import com.spotify.user.domain.event.UserRegisteredEvent;

public interface RegisterUserUseCase {
    /** Upsert user từ Kafka User.Registered (idempotent theo id). */
    void execute(UserRegisteredEvent event);
}
