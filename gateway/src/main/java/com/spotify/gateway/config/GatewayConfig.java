package com.spotify.gateway.config;

import com.spotify.gateway.filter.JwtAuthFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder, JwtAuthFilter authFilter) {
        return builder.routes()
                .route("auth-service-login", r -> r.path("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh")
                        .uri("http://localhost:8080")) // Forward to Auth Service without JWT check
                .route("auth-service-protected", r -> r.path("/api/v1/auth/**")
                        .and().not(p -> p.path("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh"))
                        .filters(f -> f.filter(authFilter))
                        .uri("http://localhost:8080"))
                .route("playlist-service", r -> r.path("/api/v1/playlists/**")
                        .filters(f -> f.filter(authFilter))
                        .uri("http://localhost:8080")) // Currently in the same monolith but routeable
                .build();
    }
}
