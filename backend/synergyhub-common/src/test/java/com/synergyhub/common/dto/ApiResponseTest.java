package com.synergyhub.common.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApiResponseTest {

    @Test
    void success_WithData_ShouldCreateSuccessResponse() {
        String data = "Success data";
        ApiResponse<String> response = ApiResponse.success(data);

        assertTrue(response.isSuccess());
        assertEquals(data, response.getData());
        assertNotNull(response.getTimestamp());
        assertNull(response.getMessage());
    }

    @Test
    void success_WithMessageAndData_ShouldCreateSuccessResponse() {
        String data = "Success data";
        String message = "Operation completed";
        ApiResponse<String> response = ApiResponse.success(message, data);

        assertTrue(response.isSuccess());
        assertEquals(data, response.getData());
        assertEquals(message, response.getMessage());
        assertNotNull(response.getTimestamp());
    }

    @Test
    void error_WithMessage_ShouldCreateErrorResponse() {
        String message = "Error occurred";
        ApiResponse<String> response = ApiResponse.error(message);

        assertFalse(response.isSuccess());
        assertEquals(message, response.getMessage());
        assertNull(response.getData());
        assertNotNull(response.getTimestamp());
    }
}