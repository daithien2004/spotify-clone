package com.spotify.auth.presentation.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.auth.application.usecase.ForgotPasswordUseCase;
import com.spotify.auth.application.usecase.EnrollTwoFactorUseCase;
import com.spotify.auth.application.usecase.LoginUseCase;
import com.spotify.auth.application.usecase.VerifyTwoFactorLoginUseCase;
import com.spotify.auth.application.usecase.LogoutUseCase;
import com.spotify.auth.application.usecase.RefreshTokenUseCase;
import com.spotify.auth.application.usecase.RegisterUseCase;
import com.spotify.auth.application.usecase.RequestEmailVerificationUseCase;
import com.spotify.auth.application.usecase.ResetPasswordUseCase;
import com.spotify.auth.application.usecase.VerifyEmailUseCase;
import com.spotify.auth.application.usecase.GetCurrentUserUseCase;
import com.spotify.auth.application.usecase.VerifyTwoFactorSetupUseCase;
import com.spotify.auth.application.usecase.DisableTwoFactorUseCase;
import com.spotify.auth.application.usecase.UpdateProfileUseCase;
import com.spotify.auth.infrastructure.security.AuthCookieFactory;
import java.util.UUID;

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
    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final EnrollTwoFactorUseCase enrollTwoFactorUseCase;
    private final VerifyTwoFactorSetupUseCase verifyTwoFactorSetupUseCase;
    private final DisableTwoFactorUseCase disableTwoFactorUseCase;
    private final VerifyTwoFactorLoginUseCase verifyTwoFactorLoginUseCase;
    private final UpdateProfileUseCase updateProfileUseCase;

    @org.springframework.beans.factory.annotation.Value("${app.cookie-domain:localhost}")
    private String cookieDomain;

    @org.springframework.web.bind.annotation.GetMapping("/me")
    public ResponseEntity<GetCurrentUserUseCase.UserResponse> getCurrentUser(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        GetCurrentUserUseCase.Response result = getCurrentUserUseCase.execute(UUID.fromString(userIdStr));
        if (!result.success()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Trả payload (UserResponse) chứ không phải Response (success+data) — nếu không
        // GlobalResponseWrapper bọc thêm envelope nữa → body bị double-wrap {success,data:{success,data:{...}}}
        // khác với các endpoint khác (login/register) chỉ bọc 1 lớp.
        return ResponseEntity.ok(result.data());
    }

    @PatchMapping("/me")
    public ResponseEntity<GetCurrentUserUseCase.UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileUseCase.Request request,
            HttpServletRequest httpRequest) {
        return ResponseEntity.ok(updateProfileUseCase.execute(requiredUserId(httpRequest), request));
    }

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

        if (!result.mfaRequired()) {
            setAuthCookies(response, result.accessToken(), result.refreshToken());
        }
        return ResponseEntity.ok(result);
    }

    @PostMapping("/2fa/verify-login")
    @SecurityRequirements()
    public ResponseEntity<VerifyTwoFactorLoginUseCase.Response> verify2faLogin(
            @Valid @RequestBody VerifyTwoFactorLoginUseCase.Request request,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        String ip = getClientIp(httpRequest);
        String ua = httpRequest.getHeader("User-Agent");
        var result = verifyTwoFactorLoginUseCase.execute(
            new VerifyTwoFactorLoginUseCase.Request(request.mfaToken(), request.code(), ip, ua));
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
        response.addHeader("Set-Cookie",
            AuthCookieFactory.accessTokenCookie(accessToken, 15 * 60, cookieDomain).toString());
        response.addHeader("Set-Cookie",
            AuthCookieFactory.refreshTokenCookie(refreshToken, 7 * 24 * 60 * 60, cookieDomain).toString());
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", AuthCookieFactory.clearCookie("auth-token", cookieDomain).toString());
        response.addHeader("Set-Cookie", AuthCookieFactory.clearCookie("refresh-token", cookieDomain).toString());
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

    // ===== TOTP 2FA =====

    @PostMapping("/2fa/enroll")
    @SecurityRequirements()
    public ResponseEntity<EnrollTwoFactorUseCase.EnrollResponse> enroll2fa(HttpServletRequest request) {
        UUID userId = requiredUserId(request);
        return ResponseEntity.ok(enrollTwoFactorUseCase.execute(userId));
    }

    @PostMapping("/2fa/verify")
    @ResponseStatus(HttpStatus.OK)
    @SecurityRequirements()
    public void verify2faSetup(@Valid @RequestBody VerifyTwoFactorSetupUseCase.Request request,
                               HttpServletRequest httpRequest) {
        verifyTwoFactorSetupUseCase.execute(requiredUserId(httpRequest), request.code());
    }

    @PostMapping("/2fa/disable")
    @ResponseStatus(HttpStatus.OK)
    public void disable2fa(@Valid @RequestBody VerifyTwoFactorSetupUseCase.Request request,
                           HttpServletRequest httpRequest) {
        disableTwoFactorUseCase.execute(requiredUserId(httpRequest), request.code());
    }

    private UUID requiredUserId(HttpServletRequest request) {
        String userIdStr = request.getHeader("X-User-Id");
        if (userIdStr == null || userIdStr.isEmpty()) {
            throw new com.spotify.auth.domain.exception.DomainException("Unauthorized");
        }
        return UUID.fromString(userIdStr);
    }
}
