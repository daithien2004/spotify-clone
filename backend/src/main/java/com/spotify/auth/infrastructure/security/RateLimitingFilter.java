package com.spotify.auth.infrastructure.security;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter dùng Bucket4j (Token Bucket algorithm).
 * Bảo vệ endpoint Login và Register khỏi Brute Force attack.
 * 
 * Cấu hình: Tối đa 10 request / phút / IP cho các endpoint nhạy cảm.
 * Trong production: nên dùng Redis-backed Bucket cho multi-instance deployment.
 * 
 * Note: Đây là in-memory implementation. Với multi-pod Kubernetes,
 * cần upgrade lên bucket4j-redis để sync state qua Redis.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    // In-memory buckets per IP (đủ tốt cho single-instance dev)
    private final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    private static final int MAX_REQUESTS_PER_MINUTE = 10;

    private Bucket getOrCreateBucket(String ip) {
        return ipBuckets.computeIfAbsent(ip, key ->
                Bucket.builder()
                        .addLimit(Bandwidth.builder()
                                .capacity(MAX_REQUESTS_PER_MINUTE)
                                .refillGreedy(MAX_REQUESTS_PER_MINUTE, Duration.ofMinutes(1))
                                .build())
                        .build()
        );
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Chỉ rate-limit các endpoint nhạy cảm
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/auth/login")
                && !path.startsWith("/api/v1/auth/register")
                && !path.startsWith("/api/v1/auth/forgot-password")
                && !path.startsWith("/api/v1/auth/refresh");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = getClientIp(request);
        Bucket bucket = getOrCreateBucket(clientIp);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            log.warn("[RATE_LIMIT] IP {} exceeded rate limit on {}", clientIp, request.getRequestURI());
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"error\":\"Too many requests. Please try again in 1 minute.\",\"status\":429}"
            );
        }
    }

    private String getClientIp(HttpServletRequest request) {
        // X-Forwarded-For được set bởi load balancer / API Gateway
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
