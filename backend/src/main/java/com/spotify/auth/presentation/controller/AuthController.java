package com.spotify.auth.presentation.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.spotify.auth.application.usecase.LoginUseCase;
import com.spotify.auth.application.usecase.LogoutUseCase;
import com.spotify.auth.application.usecase.RefreshTokenUseCase;
import com.spotify.auth.application.usecase.RegisterUseCase;

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

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public RegisterUseCase.Response register(@Valid @RequestBody RegisterUseCase.Request request) {
        return registerUseCase.execute(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public LoginUseCase.Response login(@Valid @RequestBody LoginUseCase.Request request) {
        return loginUseCase.execute(request);
    }

    @PostMapping("/refresh")
    @ResponseStatus(HttpStatus.OK)
    public RefreshTokenUseCase.Response refresh(@Valid @RequestBody RefreshTokenUseCase.Request request) {
        return refreshTokenUseCase.execute(request);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(
            jakarta.servlet.http.HttpServletRequest httpRequest,
            @Valid @RequestBody LogoutUseCase.Request request
    ) {
        // If accessToken is missing in body, try to get from Authorization header
        String accessToken = request.accessToken();
        if (accessToken == null || accessToken.isBlank()) {
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            }
        }
        
        logoutUseCase.execute(new LogoutUseCase.Request(request.refreshToken(), accessToken));
    }
}
