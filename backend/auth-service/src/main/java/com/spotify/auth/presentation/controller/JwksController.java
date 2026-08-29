package com.spotify.auth.presentation.controller;

import com.spotify.auth.infrastructure.config.JwtConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * JWKS (JSON Web Key Set) Endpoint.
 * Các microservices khác (Gateway, playlist-service, ...) gọi endpoint này
 * để lấy Public Key và tự verify JWT — không cần share secret key.
 * Pattern chuẩn OAuth2/OIDC: theo RFC 7517.
 */
@RestController
@RequestMapping("/.well-known")
@RequiredArgsConstructor
public class JwksController {

    private final JwtConfig jwtConfig;
    private final ResourceLoader resourceLoader;

    @GetMapping("/jwks.json")
    public Map<String, Object> getJwks() {
        try {
            byte[] keyBytes = FileCopyUtils.copyToByteArray(
                    resourceLoader.getResource(jwtConfig.getPublicKeyPath()).getInputStream());
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey publicKey = kf.generatePublic(spec);
            RSAPublicKey rsaPublicKey = (RSAPublicKey) publicKey;

            // Trả về theo định dạng JWK chuẩn RFC 7517
            Map<String, Object> jwk = Map.of(
                    "kty", "RSA",
                    "use", "sig",
                    "alg", "RS256",
                    "n", Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(rsaPublicKey.getModulus().toByteArray()),
                    "e", Base64.getUrlEncoder().withoutPadding()
                            .encodeToString(rsaPublicKey.getPublicExponent().toByteArray())
            );

            return Map.of("keys", List.of(jwk));
        } catch (Exception e) {
            throw new RuntimeException("Failed to load public key for JWKS endpoint", e);
        }
    }
}
