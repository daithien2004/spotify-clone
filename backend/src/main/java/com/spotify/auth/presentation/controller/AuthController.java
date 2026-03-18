package com.spotify.auth.presentation.controller;

import com.spotify.auth.application.dto.AuthResponse;
import com.spotify.auth.application.dto.LoginRequest;
import com.spotify.auth.application.dto.RegisterRequest;
import com.spotify.auth.application.usecase.LoginUseCase;
import com.spotify.auth.application.usecase.RegisterUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final RegisterUseCase registerUseCase;
    private final LoginUseCase loginUseCase;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return registerUseCase.execute(request);
    }

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return loginUseCase.execute(request);
    }
}
