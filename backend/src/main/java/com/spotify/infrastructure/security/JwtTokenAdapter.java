package com.spotify.infrastructure.security;

import com.spotify.application.port.out.TokenPort;
import com.spotify.domain.entity.User;
import com.spotify.infrastructure.config.JwtConfig;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
@RequiredArgsConstructor
public class JwtTokenAdapter implements TokenPort {
    private final JwtConfig jwtConfig;

    @Override
    public String generateToken(User user) {
        SecretKey key = Keys.hmacShaKeyFor(jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(user.getEmail().value())
                .claim("id", user.getId().toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(key)
                .compact();
    }
}
