package com.synergyhub.common.dto;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Validation error response with field-level errors
 */
@Builder
public record ValidationErrorResponse(
        int status,
        String message,
        Map<String, String> errors,
        LocalDateTime timestamp,
        String path
) {
}