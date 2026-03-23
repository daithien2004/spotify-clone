package com.spotify.auth.infrastructure.security;

import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.infrastructure.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenAdapter implements TokenPort {
    private final JwtConfig jwtConfig;
    private final JwtBlacklistService jwtBlacklistService;

    @Override
    public String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(user.getEmail().value())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(key)
                .compact();
    }

    @Override
    public String generateRefreshToken() {
        return UUID.randomUUID().toString(); // Opaque token stored in DB
    }

    @Override
    public long getRefreshTokenExpirationMillis() {
        return jwtConfig.getRefreshExpiration();
    }

    @Override
    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    @Override
    public Date getExpirationFromToken(String token) {
        return getClaims(token).getExpiration();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            if (jwtBlacklistService.isBlacklisted(token)) {
                return false;
            }
            return getClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
