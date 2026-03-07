package com.spotify.presentation.controller;

import com.spotify.application.dto.AuthResponse;
import com.spotify.application.dto.LoginRequest;
import com.spotify.application.dto.RegisterRequest;
import com.spotify.application.usecase.auth.LoginUseCase;
import com.spotify.application.usecase.auth.RegisterUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
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
