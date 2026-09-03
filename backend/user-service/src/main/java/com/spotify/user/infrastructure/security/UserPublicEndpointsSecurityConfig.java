package com.spotify.user.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

/**
 * Opens public read endpoints (profile, followers, following) so clients can view
 * a user profile without a JWT. Follow/unfollow mutations stay authenticated via
 * common-lib {@code ServiceSecurityConfig} (@Order(2)) + {@code X-User-Id} header.
 */
@Configuration
public class UserPublicEndpointsSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain publicReadSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher(new OrRequestMatcher(
                        new AntPathRequestMatcher("/api/v1/users/*", "GET"),
                        new AntPathRequestMatcher("/api/v1/users/*/followers", "GET"),
                        new AntPathRequestMatcher("/api/v1/users/*/following", "GET")))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .build();
    }
}
