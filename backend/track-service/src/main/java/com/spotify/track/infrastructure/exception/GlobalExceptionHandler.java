package com.spotify.track.infrastructure.exception;

import com.spotify.common.infrastructure.web.ApiResponse;

import com.spotify.track.domain.exception.TrackAudioNotFoundException;
import com.spotify.track.domain.exception.TrackNotFoundException;

import jakarta.validation.ConstraintViolationException;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps exceptions to standardized {@link ApiResponse} error bodies (conventions.md §2).
 * Sits alongside common-lib {@code GlobalResponseWrapper} which wraps success responses.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolation(ConstraintViolationException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error("Validation failed: " + ex.getMessage()));
    }

    // A missing track is a 404 (resource not found), not a generic 500
    @ExceptionHandler(TrackNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleTrackNotFound(TrackNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(TrackAudioNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleTrackAudioNotFound(TrackAudioNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    // Use case guards (blank title/artist, negative duration) are client errors
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return ResponseEntity.internalServerError().body(ApiResponse.error("An unexpected error occurred"));
    }
}