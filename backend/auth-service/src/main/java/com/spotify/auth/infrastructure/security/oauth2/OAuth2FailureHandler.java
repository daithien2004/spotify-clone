package com.spotify.auth.infrastructure.security.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Handles OAuth2 login failure by redirecting to frontend login page with an error indicator.
 */
@Component
@Slf4j
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private static final String FAILURE_URL = "http://localhost:3000/login?error=oauth2_failed";

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        log.error("OAuth2 login failed: {}", exception.getMessage());
        getRedirectStrategy().sendRedirect(request, response, FAILURE_URL);
    }
}
