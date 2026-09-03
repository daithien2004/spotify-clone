package com.spotify.user.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.spotify.user.application.usecase.RegisterUserUseCase;
import com.spotify.user.domain.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumes User.Registered from auth-service (topic {@code user.registered}).
 * auth-service serializes with JsonSerializer → value type StringDeserializer nhận JSON text,
 * parse tay qua ObjectMapper để tránh spring.json.trusted.packages/type-header complexity.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.enabled", havingValue = "true", matchIfMissing = true)
public class UserRegisteredConsumer {

    private static final String USER_REGISTERED_TOPIC = "user.registered";

    private final RegisterUserUseCase registerUserUseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = USER_REGISTERED_TOPIC, groupId = "user-service-group")
    public void onUserRegistered(String payload) {
        try {
            UserRegisteredEvent event = objectMapper.readValue(payload, UserRegisteredEvent.class);
            log.info("[USER_REGISTERED] userId={} email={}", event.userId(), event.email());
            registerUserUseCase.execute(event);
        } catch (Exception ex) {
            // Không đánh sập consumer loop — log và bỏ qua record lỗi (retry/DLQ ngoài scope).
            log.error("[USER_REGISTERED] Failed to process event: {}", ex.getMessage());
        }
    }
}
