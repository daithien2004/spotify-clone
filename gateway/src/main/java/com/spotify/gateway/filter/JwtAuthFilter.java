package com.spotify.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.io.Resource;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.List;

/**
 * Gateway filter for JWT authentication with RSA validation and Redis blacklist support.
 * Supports both Authorization header and HttpOnly cookies.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter implements GatewayFilter {

    private final ObjectMapper objectMapper;
    private final ReactiveStringRedisTemplate redisTemplate;

    @Value("${jwt.public-key-path}")
    private Resource publicKeyResource;

    private PublicKey publicKey;

    @PostConstruct
    public void init() {
        try {
            byte[] keyBytes = FileCopyUtils.copyToByteArray(publicKeyResource.getInputStream());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            this.publicKey = KeyFactory.getInstance("RSA").generatePublic(spec);
            log.info("RSA Public Key loaded successfully at Gateway startup");
        } catch (Exception e) {
            log.error("Critical error: Could not load RSA Public Key", e);
            throw new RuntimeException("Could not load RSA Public Key", e);
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Strip sensitive headers from client to prevent spoofing
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> {
                    h.remove("X-User-Id");
                    h.remove("X-User-Email");
                    h.remove("X-User-Roles");
                })
                .build();
        
        ServerWebExchange mutatedExchange = exchange.mutate().request(request).build();

        String token = extractToken(request);
        if (token == null) {
            return unauthorized(mutatedExchange, "Missing Authentication Token");
        }

        String tokenHash = DigestUtils.sha256Hex(token);

        return checkBlacklist(tokenHash)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        return unauthorized(mutatedExchange, "Token has been revoked");
                    }
                    return parseToken(token)
                            .flatMap(claims -> buildAuthenticatedRequest(claims, mutatedExchange, chain))
                            .onErrorResume(e -> handleJwtError(e, mutatedExchange));
                });
    }

    private String extractToken(ServerHttpRequest request) {
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }

        HttpCookie cookie = request.getCookies().getFirst("auth-token");
        if (cookie != null) {
            return cookie.getValue();
        }

        return null;
    }

    private Mono<Boolean> checkBlacklist(String tokenHash) {
        return redisTemplate.hasKey("blacklist:" + tokenHash);
    }

    private Mono<Claims> parseToken(String token) {
        return Mono.fromCallable(() -> Jwts.parser()
                .verifyWith(publicKey)
                .build()
                .parseSignedClaims(token)
                .getPayload());
    }

    private Mono<Void> handleJwtError(Throwable e, ServerWebExchange exchange) {
        if (e instanceof ExpiredJwtException) {
            log.warn("JWT Expired");
            return unauthorized(exchange, "Token has expired");
        } else if (e instanceof SignatureException) {
            log.warn("Invalid JWT signature");
            return unauthorized(exchange, "Invalid token signature");
        } else if (e instanceof MalformedJwtException) {
            log.warn("Malformed JWT");
            return unauthorized(exchange, "Malformed token format");
        } else {
            log.error("JWT validation error: {}", e.getClass().getSimpleName());
            return unauthorized(exchange, "Authentication failed");
        }
    }

    private Mono<Void> buildAuthenticatedRequest(Claims claims, 
                                                 ServerWebExchange exchange, 
                                                 GatewayFilterChain chain) {
        try {
            String userId = claims.get("id", String.class);
            String email = claims.getSubject();
            
            if (userId == null || userId.isBlank()) {
                log.warn("JWT validation failed: missing user id claim");
                return unauthorized(exchange, "Invalid token: missing user identity");
            }

            List<?> roles = claims.get("roles", List.class);
            String rolesJson = roles != null ? objectMapper.writeValueAsString(roles) : "[]";

            ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Email", email)
                    .header("X-User-Roles", rolesJson)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        } catch (Exception e) {
            log.error("Failed to build authenticated request: {}", e.getClass().getSimpleName());
            return unauthorized(exchange, "Authentication failed");
        }
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        log.warn("Unauthorized request handled: {}", message);
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String body = String.format("{\"error\": \"UNAUTHORIZED\", \"message\": \"%s\"}", message);
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        
        return response.writeWith(Mono.just(buffer));
    }
}
