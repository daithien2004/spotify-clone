package com.spotify.track.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "spring.minio")
public record MinioProperties(
    String endpoint,
    String accessKey,
    String secretKey,
    String bucket
) {
}