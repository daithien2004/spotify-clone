package com.spotify.application.dto;

public record AuthResponse(String token, String email, String displayName, String avatarUrl) {}
