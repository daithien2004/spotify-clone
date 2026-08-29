package com.spotify.common.infrastructure.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GatewayHeaderFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String userId = request.getHeader("X-User-Id");
        String userEmail = request.getHeader("X-User-Email");
        String rolesHeader = request.getHeader("X-User-Roles");

        if (userId != null && !userId.isEmpty()) {
            List<String> roles = new ArrayList<>();
            if (rolesHeader != null && !rolesHeader.isEmpty()) {
                try {
                    if (rolesHeader.trim().startsWith("[")) {
                        roles = objectMapper.readValue(rolesHeader, new TypeReference<List<String>>() {});
                    } else {
                        roles = List.of(rolesHeader.split(","));
                    }
                } catch (Exception e) {
                    log.error("Failed to parse roles header: {}", rolesHeader, e);
                }
            }

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .filter(role -> role != null && !role.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());

            // Use userId as the principal
            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(auth);
            log.debug("Authenticated user {} ({}) with roles {}", userId, userEmail, roles);
        }

        filterChain.doFilter(request, response);
    }
}
