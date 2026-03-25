package com.spotify.auth.presentation.controller;

import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.auth.application.usecase.ForgotPasswordUseCase;
import com.spotify.auth.application.usecase.LoginUseCase;
import com.spotify.auth.application.usecase.LogoutUseCase;
import com.spotify.auth.application.usecase.RefreshTokenUseCase;
import com.spotify.auth.application.usecase.RegisterUseCase;
import com.spotify.auth.application.usecase.RequestEmailVerificationUseCase;
import com.spotify.auth.application.usecase.ResetPasswordUseCase;
import com.spotify.auth.application.usecase.TwoFactorAuthUseCase;
import com.spotify.auth.application.usecase.VerifyEmailUseCase;

import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import jakarta.servlet.http.HttpServletRequest;
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
    private final TwoFactorAuthUseCase twoFactorAuthUseCase;

    // ===== CORE AUTH =====

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @SecurityRequirements()
    public RegisterUseCase.Response register(@Valid @RequestBody RegisterUseCase.Request request) {
        return registerUseCase.execute(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirements()
    public LoginUseCase.Response login(@Valid @RequestBody LoginUseCase.Request request,
                                       HttpServletRequest httpRequest) {
        // Inject IP và UserAgent từ HTTP request
        String ip = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        return loginUseCase.execute(new LoginUseCase.Request(
                request.email(), request.password(), ip, userAgent));
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirements()
    public RefreshTokenUseCase.Response refresh(@Valid @RequestBody RefreshTokenUseCase.Request request,
                                                HttpServletRequest httpRequest) {
        return refreshTokenUseCase.execute(new RefreshTokenUseCase.Request(
                request.refreshToken(), getClientIp(httpRequest), httpRequest.getHeader("User-Agent")));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest httpRequest,
                       @Valid @RequestBody LogoutUseCase.Request request) {
        String accessToken = null;
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            accessToken = authHeader.substring(7);
        }
        logoutUseCase.execute(request, accessToken);
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

    // ===== TWO-FACTOR AUTHENTICATION =====

    @PostMapping("/2fa/setup")
    @ResponseStatus(HttpStatus.OK)
    public TwoFactorAuthUseCase.SetupResponse setup2fa(@RequestParam UUID userId) {
        // TODO: Trong production, lấy userId từ SecurityContext thay vì request param
        return twoFactorAuthUseCase.setupTwoFa(userId);
    }

    @PostMapping("/2fa/confirm")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> confirm2fa(@RequestParam UUID userId,
                                          @RequestParam String totpCode) {
        twoFactorAuthUseCase.confirmTwoFa(userId, totpCode);
        return Map.of("message", "Two-Factor Authentication enabled successfully.");
    }

    @PostMapping("/2fa/disable")
    @ResponseStatus(HttpStatus.OK)
    public Map<String, String> disable2fa(@RequestParam UUID userId,
                                          @RequestParam String totpCode) {
        twoFactorAuthUseCase.disableTwoFa(userId, totpCode);
        return Map.of("message", "Two-Factor Authentication disabled.");
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
