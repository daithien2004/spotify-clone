package com.spotify.playlist.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.spotify.playlist.domain.service.LexoRankService;

/**
 * Infrastructure wiring for pure domain services. Keeps domain/ free of Spring
 * annotations while still exposing services as injectable beans.
 */
@Configuration
public class PlaylistInfrastructureConfig {

    @Bean
    public LexoRankService lexoRankService() {
        return new LexoRankService();
    }
}