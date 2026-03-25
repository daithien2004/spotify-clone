package com.spotify.auth.infrastructure.security;

import com.spotify.auth.application.port.out.SecurityTokenPort;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Infrastructure adapter: Lưu và đọc One-Time Security Tokens từ Redis.
 * Key pattern: "security_token:{tokenType}:{token}" → userId
 * Redis TTL được đặt theo yêu cầu của từng use case.
 */
@Component
@RequiredArgsConstructor
public class RedisSecurityTokenAdapter implements SecurityTokenPort {

    private final StringRedisTemplate redisTemplate;

    private String buildKey(String token, String tokenType) {
        return "security_token:" + tokenType + ":" + token;
    }

    @Override
    public void save(String token, UUID userId, String tokenType, long ttlSeconds) {
        redisTemplate.opsForValue().set(
                buildKey(token, tokenType),
                userId.toString(),
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }

    @Override
    public Optional<UUID> findUserIdByToken(String token, String tokenType) {
        String value = redisTemplate.opsForValue().get(buildKey(token, tokenType));
        if (value == null) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public void delete(String token, String tokenType) {
        redisTemplate.delete(buildKey(token, tokenType));
    }
}
