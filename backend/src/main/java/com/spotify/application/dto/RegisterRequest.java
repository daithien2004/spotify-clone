package com.spotify.application.dto;

public record RegisterRequest(String email, String password, String displayName, String avatarUrl) {}
