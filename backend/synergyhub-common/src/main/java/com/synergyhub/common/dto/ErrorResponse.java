package com.synergyhub.common.dto;

import lombok.Builder;

import java.time.LocalDateTime;

/**
 * Error response record for exception handling
 */
@Builder
public record ErrorResponse(
        int status,
        String message,
        LocalDateTime timestamp,
        String path
) {
    public ErrorResponse(int status, String message, LocalDateTime timestamp) {
        this(status, message, timestamp, null);
    }

    /**
     * Create error response without path
     */
    public static ErrorResponse of(int status, String message) {
        return new ErrorResponse(status, message, LocalDateTime.now(), null);
    }

    /**
     * Create error response with path
     */
    public static ErrorResponse of(int status, String message, String path) {
        return new ErrorResponse(status, message, LocalDateTime.now(), path);
    }
}