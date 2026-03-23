package com.spotify.auth.infrastructure.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class RedisConfig {
    // Spring Boot automatically configures RedisCacheManager with spring-boot-starter-data-redis.
    // @EnableCaching is sufficient for generic @Cacheable usage.
}
