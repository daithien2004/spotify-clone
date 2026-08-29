package com.spotify.common.infrastructure.web;

import java.time.OffsetDateTime;

import lombok.Builder;

/**
 * Standard API Response envelope for all success and error responses.
 */
@Builder
public record ApiResponse<T>(
    boolean success,
    T data,
    String message,
    OffsetDateTime timestamp
) {
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .timestamp(OffsetDateTime.now())
                .build();
    }

    public static <T> ApiResponse<T> error(String message, T data) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .data(data)
                .timestamp(OffsetDateTime.now())
                .build();
    }
}
