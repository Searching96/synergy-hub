package com.synergyhub.common.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ErrorResponseTest {

    @Test
    void of_WithStatusAndMessage_ShouldCreateErrorResponse() {
        ErrorResponse response = ErrorResponse.of(404, "Not found");

        assertEquals(404, response.status());
        assertEquals("Not found", response.message());
        assertNotNull(response.timestamp());
        assertNull(response.path());
    }

    @Test
    void of_WithStatusMessageAndPath_ShouldCreateErrorResponse() {
        ErrorResponse response = ErrorResponse.of(400, "Bad request", "/api/test");

        assertEquals(400, response.status());
        assertEquals("Bad request", response.message());
        assertEquals("/api/test", response.path());
        assertNotNull(response.timestamp());
    }

    @Test
    void constructor_WithThreeParameters_ShouldSetPathToNull() {
        LocalDateTime now = LocalDateTime.now();
        ErrorResponse response = new ErrorResponse(500, "Internal error", now);

        assertEquals(500, response.status());
        assertEquals("Internal error", response.message());
        assertEquals(now, response.timestamp());
        assertNull(response.path());
    }

    @Test
    void constructor_WithAllParameters_ShouldSetAllFields() {
        LocalDateTime now = LocalDateTime.now();
        ErrorResponse response = new ErrorResponse(403, "Forbidden", now, "/api/admin");

        assertEquals(403, response.status());
        assertEquals("Forbidden", response.message());
        assertEquals(now, response.timestamp());
        assertEquals("/api/admin", response.path());
    }
}