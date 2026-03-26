package com.spotify.auth.presentation.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.auth.application.usecase.ForgotPasswordUseCase;
import com.spotify.auth.application.usecase.LoginUseCase;
import com.spotify.auth.application.usecase.LogoutUseCase;
import com.spotify.auth.application.usecase.RefreshTokenUseCase;
import com.spotify.auth.application.usecase.RegisterUseCase;
import com.spotify.auth.application.usecase.RequestEmailVerificationUseCase;
import com.spotify.auth.application.usecase.ResetPasswordUseCase;
import com.spotify.auth.application.usecase.VerifyEmailUseCase;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;
    private final RefreshTokenUseCase refreshTokenUseCase;
    private final LogoutUseCase logoutUseCase;
    private final RequestEmailVerificationUseCase requestEmailVerificationUseCase;
    private final VerifyEmailUseCase verifyEmailUseCase;
    private final ForgotPasswordUseCase forgotPasswordUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;
 
    @org.springframework.beans.factory.annotation.Value("${app.cookie-domain:localhost}")
    private String cookieDomain;

    @PostMapping("/register")
    @SecurityRequirements()
    public ResponseEntity<RegisterUseCase.Response> register(@Valid @RequestBody RegisterUseCase.Request request,
                                                               HttpServletResponse response) {
        RegisterUseCase.Response result = registerUseCase.execute(request);
        setAuthCookies(response, result.accessToken(), result.refreshToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    @PostMapping("/login")
    @SecurityRequirements()
    public ResponseEntity<LoginUseCase.Response> login(@Valid @RequestBody LoginUseCase.Request request,
                                                       HttpServletRequest httpRequest,
                                                       HttpServletResponse response) {
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        LoginUseCase.Response result = loginUseCase.execute(new LoginUseCase.Request(
                request.email(), request.password(), ip, userAgent));
        
        setAuthCookies(response, result.accessToken(), result.refreshToken());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/refresh")
    @SecurityRequirements()
    public ResponseEntity<RefreshTokenUseCase.Response> refresh(HttpServletRequest httpRequest,
                                                                 HttpServletResponse response) {
        String refreshToken = getCookieValue(httpRequest, "refresh-token");
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
 
        RefreshTokenUseCase.Response result = refreshTokenUseCase.execute(new RefreshTokenUseCase.Request(
                refreshToken, getClientIp(httpRequest), httpRequest.getHeader("User-Agent")));
        
        setAuthCookies(response, result.accessToken(), result.refreshToken());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest, HttpServletResponse response) {
        String refreshToken = getCookieValue(httpRequest, "refresh-token");
        String accessToken = getCookieValue(httpRequest, "auth-token");
        
        logoutUseCase.execute(new LogoutUseCase.Request(refreshToken != null ? refreshToken : ""), accessToken);
        
        clearAuthCookies(response);
    }

    private void setAuthCookies(HttpServletResponse response, String accessToken, String refreshToken) {
        // Access Token Cookie (shorter life)
        ResponseCookie accessCookie = ResponseCookie.from("auth-token", accessToken)
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .path("/")
                .domain(cookieDomain)
                .maxAge(15 * 60)
                .sameSite("Lax")
                .build();
 
        // Refresh Token Cookie (longer life)
        ResponseCookie refreshCookie = ResponseCookie.from("refresh-token", refreshToken)
                .httpOnly(true)
                .secure(false) // Set to true in production with HTTPS
                .path("/")
                .domain(cookieDomain)
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("Lax")
                .build();
 
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }
 
    private void clearAuthCookies(HttpServletResponse response) {
        ResponseCookie accessCookie = ResponseCookie.from("auth-token", "")
                .httpOnly(true)
                .path("/")
                .domain(cookieDomain)
                .maxAge(0)
                .build();
        ResponseCookie refreshCookie = ResponseCookie.from("refresh-token", "")
                .httpOnly(true)
                .path("/")
                .domain(cookieDomain)
                .maxAge(0)
                .build();
        response.addHeader("Set-Cookie", accessCookie.toString());
        response.addHeader("Set-Cookie", refreshCookie.toString());
    }
 
    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (var cookie : request.getCookies()) {
            if (cookie.getName().equals(name)) return cookie.getValue();
        }
        return null;
    }

    // ===== EMAIL VERIFICATION =====

    @PostMapping("/send-verification")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirements()
    public Map<String, String> sendVerification(@Valid @RequestBody RequestEmailVerificationUseCase.Request request) {
        requestEmailVerificationUseCase.execute(request);
        return Map.of("message", "Verification email sent. Please check your inbox.");
    }

    @PostMapping("/verify-email")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirements()
    public Map<String, String> verifyEmail(@Valid @RequestBody VerifyEmailUseCase.Request request) {
        verifyEmailUseCase.execute(request);
        return Map.of("message", "Email verified successfully!");
    }

    // ===== FORGOT / RESET PASSWORD =====

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirements()
    public Map<String, String> forgotPassword(@Valid @RequestBody ForgotPasswordUseCase.Request request) {
        forgotPasswordUseCase.execute(request);
        // Luôn trả về success kể cả khi email không tồn tại (chống enumeration attack)
        return Map.of("message", "If this email is registered, a reset link has been sent.");
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirements()
    public Map<String, String> resetPassword(@Valid @RequestBody ResetPasswordUseCase.Request request) {
        resetPasswordUseCase.execute(request);
        return Map.of("message", "Password reset successfully. Please login with your new password.");
    }

    // Helper: Lấy IP thực của client (qua load balancer / Gateway)
    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
