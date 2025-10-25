package com.synergyhub.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ExceptionTest {

    @Test
    void resourceNotFoundException_WithMessage_ShouldCreateException() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");
        assertEquals("User not found", ex.getMessage());
    }

    @Test
    void resourceNotFoundException_WithResourceFieldValue_ShouldFormatMessage() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User", "id", 123);
        assertEquals("User not found with id: '123'", ex.getMessage());
    }

    @Test
    void badRequestException_ShouldCreateException() {
        InvalidRequestException ex = new InvalidRequestException("Invalid input");
        assertEquals("Invalid input", ex.getMessage());
    }

    @Test
    void unauthorizedException_ShouldCreateException() {
        UnauthorizedException ex = new UnauthorizedException("Not authenticated");
        assertEquals("Not authenticated", ex.getMessage());
    }

    @Test
    void forbiddenException_ShouldCreateException() {
        ForbiddenException ex = new ForbiddenException("Access denied");
        assertEquals("Access denied", ex.getMessage());
    }

    @Test
    void conflictException_ShouldCreateException() {
        ConflictException ex = new ConflictException("Resource exists");
        assertEquals("Resource exists", ex.getMessage());
    }

    @Test
    void emailAlreadyExistsException_ShouldFormatMessage() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("test@example.com");
        assertTrue(ex.getMessage().contains("test@example.com"));
        assertTrue(ex.getMessage().contains("already registered"));
    }

    @Test
    void accountLockedException_ShouldCreateException() {
        AccountLockedException ex = new AccountLockedException("Account locked");
        assertEquals("Account locked", ex.getMessage());
    }

    @Test
    void invalidTwoFactorCodeException_ShouldCreateException() {
        InvalidTwoFactorCodeException ex = new InvalidTwoFactorCodeException("Invalid code");
        assertEquals("Invalid code", ex.getMessage());
    }

    @Test
    void invalidTokenException_ShouldCreateException() {
        InvalidTokenException ex = new InvalidTokenException("Token expired");
        assertEquals("Token expired", ex.getMessage());
    }
}