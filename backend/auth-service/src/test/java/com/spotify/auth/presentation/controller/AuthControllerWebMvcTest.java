package com.spotify.auth.presentation.controller;

import com.spotify.auth.application.usecase.DisableTwoFactorUseCase;
import com.spotify.auth.application.usecase.EnrollTwoFactorUseCase;
import com.spotify.auth.application.usecase.ForgotPasswordUseCase;
import com.spotify.auth.application.usecase.GetCurrentUserUseCase;
import com.spotify.auth.application.usecase.LoginUseCase;
import com.spotify.auth.application.usecase.LogoutUseCase;
import com.spotify.auth.application.usecase.RefreshTokenUseCase;
import com.spotify.auth.application.usecase.RegisterUseCase;
import com.spotify.auth.application.usecase.RequestEmailVerificationUseCase;
import com.spotify.auth.application.usecase.ResetPasswordUseCase;
import com.spotify.auth.application.usecase.UpdateProfileUseCase;
import com.spotify.auth.application.usecase.VerifyEmailUseCase;
import com.spotify.auth.application.usecase.VerifyTwoFactorLoginUseCase;
import com.spotify.auth.application.usecase.VerifyTwoFactorSetupUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvcTest cho AuthController — xác minh contract API qua MockMvc ở tầng HTTP:
 * envelope {@code {success,data,...}} + @Valid validation + HTTP status.
 *
 * <p>Đây LÀ regression-test cho bug "/me double-wrap" (global wrapper bọc envenlope
 * thêm 1 lớp nữa → body thành {success,data:{success,data:{...}}}). Test dưới assert
 * JSON path {@code data.id} chạy thẳng — nếu double-wrap lại, nó sẽ fail.
 *
 * <p>Chỉ load tầng web (controller + GlobalResponseWrapper + GlobalExceptionHandler),
 * KHÔNG load security/JPA/Redis/Kafka — các use case được mock bằng @MockBean.
 */
@WebMvcTest(AuthController.class)
// Tắt security filter mặc định (default chain yêu cầu auth toàn bộ → 302/403).
// Test này chỉ kiểm contract HTTP + envelope + validation; security riêng (JWT/gateway).
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private RegisterUseCase registerUseCase;
    @MockBean private LoginUseCase loginUseCase;
    @MockBean private RefreshTokenUseCase refreshTokenUseCase;
    @MockBean private LogoutUseCase logoutUseCase;
    @MockBean private RequestEmailVerificationUseCase requestEmailVerificationUseCase;
    @MockBean private VerifyEmailUseCase verifyEmailUseCase;
    @MockBean private ForgotPasswordUseCase forgotPasswordUseCase;
    @MockBean private ResetPasswordUseCase resetPasswordUseCase;
    @MockBean private GetCurrentUserUseCase getCurrentUserUseCase;
    @MockBean private EnrollTwoFactorUseCase enrollTwoFactorUseCase;
    @MockBean private VerifyTwoFactorSetupUseCase verifyTwoFactorSetupUseCase;
    @MockBean private DisableTwoFactorUseCase disableTwoFactorUseCase;
    @MockBean private VerifyTwoFactorLoginUseCase verifyTwoFactorLoginUseCase;
    @MockBean private UpdateProfileUseCase updateProfileUseCase;

    @Test
    void should_ReturnSingleWrappedUser_when_GetMeWithXUserHeader() throws Exception {
        UUID id = UUID.randomUUID();
        GetCurrentUserUseCase.UserResponse user = new GetCurrentUserUseCase.UserResponse(
                id.toString(), "alice@example.com", "Alice", null, true, false);
        when(getCurrentUserUseCase.execute(any(UUID.class)))
                .thenReturn(new GetCurrentUserUseCase.Response(true, user));

        mockMvc.perform(get("/api/v1/auth/me").header("X-User-Id", id.toString()))
                .andExpect(status().isOk())
                // Envelope ngoài 1 lớp — data trỏ thẳng tới payload, KHÔNG có data.data (regression /me double-wrap)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(id.toString()))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"))
                .andExpect(jsonPath("$.data.displayName").value("Alice"))
                .andExpect(jsonPath("$.data.emailVerified").value(true));
    }

    @Test
    void should_Return401_when_GetMeWithoutXUserHeader() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void should_Return400_when_RegisterBodyInvalid() throws Exception {
        // Thiếu displayName + email sai định dạng → @Valid bắt, GlobalExceptionHandler → 400
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void should_Return201Wrapped_when_RegisterSucceeds() throws Exception {
        RegisterUseCase.Response response = new RegisterUseCase.Response(
                "access-token", "refresh-token",
                UUID.randomUUID().toString(), "alice@example.com", "Alice", null, 900000L);
        when(registerUseCase.execute(any(RegisterUseCase.Request.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"alice@example.com\",\"password\":\"StrongPass2026!\",\"displayName\":\"Alice\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("alice@example.com"));
    }
}
