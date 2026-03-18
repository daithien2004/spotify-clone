package com.spotify.auth.application.dto;

public record AuthResponse(
    String token,
    String email,
    String displayName,
    String avatarUrl
) {}
