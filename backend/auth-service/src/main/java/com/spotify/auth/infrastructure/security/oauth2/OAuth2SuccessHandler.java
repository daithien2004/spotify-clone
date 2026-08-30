package com.spotify.auth.infrastructure.security.oauth2;

import com.spotify.auth.domain.entity.User;
import com.spotify.auth.domain.entity.RefreshToken;
import com.spotify.auth.application.port.out.TokenPort;
import com.spotify.auth.domain.repository.RefreshTokenRepository;
import com.spotify.auth.infrastructure.security.AuthCookieFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @org.springframework.beans.factory.annotation.Value("${app.cookie-domain:localhost}")
    private String cookieDomain;

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
        // Đồng bộ với AuthController: path /, domain, sameSite Lax (ADR D5)
        response.addHeader("Set-Cookie", AuthCookieFactory.accessTokenCookie(
            accessToken, accessExpiresIn / 1000, cookieDomain).toString());
        response.addHeader("Set-Cookie", AuthCookieFactory.refreshTokenCookie(
            refreshTokenStr, refreshExpiresIn / 1000, cookieDomain).toString());

        log.info("OAuth2 login successful for user: {}", user.getEmail().value());

        // 4. Redirect to frontend — NO tokens in URL
        getRedirectStrategy().sendRedirect(request, response, FRONTEND_CALLBACK_URL);
    }
}
