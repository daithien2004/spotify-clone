package com.spotify.auth.infrastructure.security;

import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.entity.User;
import com.spotify.auth.infrastructure.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenAdapter implements TokenPort {
    private final JwtConfig jwtConfig;
    private final ResourceLoader resourceLoader;

    private PrivateKey getPrivateKey() {
        try {
            byte[] keyBytes = FileCopyUtils.copyToByteArray(resourceLoader.getResource(jwtConfig.getPrivateKeyPath()).getInputStream());
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePrivate(spec);
        } catch (Exception e) {
            throw new RuntimeException("Could not load private key", e);
        }
    }

    private PublicKey getPublicKey() {
        try {
            byte[] keyBytes = FileCopyUtils.copyToByteArray(resourceLoader.getResource(jwtConfig.getPublicKeyPath()).getInputStream());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            return kf.generatePublic(spec);
        } catch (Exception e) {
            throw new RuntimeException("Could not load public key", e);
        }
    }

    @Override
    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getEmail().value())
                .claim("id", user.getId().toString())
                .claim("roles", user.getRoles())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .signWith(getPrivateKey())
                .compact();
    }

    @Override
    public long getAccessTokenExpirationMillis() {
        return jwtConfig.getExpiration();
    }

    @Override
    public String generateRefreshToken() {
        return UUID.randomUUID().toString();
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
            return getClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
