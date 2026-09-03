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
                // OAuth2 endpoints - bypass JWT filter entirely
                .route("auth-oauth2", r -> r.path("/oauth2/**", "/login/oauth2/**")
                        .uri("http://localhost:8081"))
                .route("auth-service-login", r -> r.path(
                        "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh",
                        "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                        "/api/v1/auth/send-verification", "/api/v1/auth/verify-email",
                        "/api/v1/auth/2fa/verify-login")
                        .uri("http://localhost:8081")) // Forward to Auth Service without JWT check
                .route("auth-service-protected", r -> r.path("/api/v1/auth/**")
                        .and().not(p -> p.path(
                            "/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh",
                            "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password",
                            "/api/v1/auth/send-verification", "/api/v1/auth/verify-email",
                            "/api/v1/auth/2fa/verify-login"))
                        .filters(f -> f.filter(authFilter))
                        .uri("http://localhost:8081"))
                .route("playlist-service", r -> r.path("/api/v1/playlists/**")
                        .filters(f -> f.filter(authFilter))
                        .uri("http://localhost:8084"))
                .route("track-service", r -> r.path("/api/v1/tracks/**")
                        .filters(f -> f.filter(authFilter))
                        .uri("http://localhost:8085"))
                .route("search-service", r -> r.path("/api/v1/search/**")
                        .filters(f -> f.filter(authFilter))
                        .uri("http://localhost:8086"))
                // user-service public reads (profile/followers/following) — no JWT needed
                .route("user-service-public", r -> r.path("/api/v1/users/*/followers",
                                "/api/v1/users/*/following", "/api/v1/users/*")
                        .uri("http://localhost:8088"))
                // user-service mutations (follow/unfollow) — require JWT → X-User-Id
                .route("user-service", r -> r.path("/api/v1/users/**")
                        .filters(f -> f.filter(authFilter))
                        .uri("http://localhost:8088"))
                .build();
    }
}
