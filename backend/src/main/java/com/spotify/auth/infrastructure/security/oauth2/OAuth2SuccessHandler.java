package com.spotify.auth.infrastructure.security.oauth2;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.entity.RefreshToken;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Handles successful OAuth2 authentication by issuing JWT cookies and redirecting to the frontend.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final TokenPort tokenPort;
    private final RefreshTokenRepository refreshTokenRepository;

    private static final String FRONTEND_CALLBACK_URL = "http://localhost:3000/oauth2/callback";

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2Principal principal = (CustomOAuth2Principal) authentication.getPrincipal();
        User user = principal.getUser();

        // 1. Generate tokens
        String accessToken = tokenPort.generateToken(user);
        String refreshTokenStr = tokenPort.generateRefreshToken();
        long accessExpiresIn = tokenPort.getAccessTokenExpirationMillis();
        long refreshExpiresIn = tokenPort.getRefreshTokenExpirationMillis();

        // 2. Persist refresh token (hashed by RefreshTokenRepositoryImpl)
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .userId(user.getId())
                .familyId(UUID.randomUUID())
                .expiresAt(OffsetDateTime.now().plus(Duration.ofMillis(refreshExpiresIn)))
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        refreshTokenRepository.save(refreshToken);

        // 3. Set HttpOnly cookies
        ResponseCookie accessCookie = ResponseCookie.from("auth-token", accessToken)
                .httpOnly(true)
                .secure(false) // Set to true in production
                .path("/")
                .maxAge(accessExpiresIn / 1000)
                .sameSite("Lax")  // Lax is required for OAuth2 redirects
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh-token", refreshTokenStr)
                .httpOnly(true)
                .secure(false)
                .path("/api/v1/auth/refresh")
                .maxAge(refreshExpiresIn / 1000)
                .sameSite("Lax")
                .build();

        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());

        log.info("OAuth2 login successful for user: {}", user.getEmail().value());

        // 4. Redirect to frontend — NO tokens in URL
        getRedirectStrategy().sendRedirect(request, response, FRONTEND_CALLBACK_URL);
    }
}
