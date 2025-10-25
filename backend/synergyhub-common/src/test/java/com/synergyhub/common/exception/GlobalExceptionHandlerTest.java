package com.synergyhub.common.exception;

import com.synergyhub.common.dto.ErrorResponse;
import com.synergyhub.common.dto.ValidationErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @InjectMocks
    private GlobalExceptionHandler exceptionHandler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @Test
    void handleResourceNotFound_ShouldReturnNotFoundResponse() {
        ResourceNotFoundException ex = new ResourceNotFoundException("User not found");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleResourceNotFound(ex, request);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("User not found", response.getBody().message());
        assertEquals(404, response.getBody().status());
        assertEquals("/api/test", response.getBody().path());
        assertNotNull(response.getBody().timestamp());
    }

    @Test
    void handleEmailAlreadyExists_ShouldReturnConflictResponse() {
        EmailAlreadyExistsException ex = new EmailAlreadyExistsException("test@example.com");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleEmailAlreadyExists(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(409, response.getBody().status());
        assertTrue(response.getBody().message().contains("test@example.com"));
    }

    @Test
    void handleAccountLocked_ShouldReturnLockedResponse() {
        AccountLockedException ex = new AccountLockedException("Account is locked");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleAccountLocked(ex, request);

        assertEquals(HttpStatus.LOCKED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Account is locked", response.getBody().message());
        assertEquals(423, response.getBody().status());
    }

    @Test
    void handleBadCredentials_ShouldReturnUnauthorizedResponse() {
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleBadCredentials(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid email or password", response.getBody().message());
        assertEquals(401, response.getBody().status());
    }

    @Test
    void handleForbidden_ShouldReturnForbiddenResponse() {
        ForbiddenException ex = new ForbiddenException("Access denied");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleForbidden(ex, request);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Access denied", response.getBody().message());
        assertEquals(403, response.getBody().status());
    }

    @Test
    void handleGenericException_ShouldReturnInternalServerError() {
        Exception ex = new Exception("Unexpected error");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleGenericException(ex, request);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(500, response.getBody().status());
        assertNotNull(response.getBody().message());
    }

    @Test
    void handleInvalidToken_ShouldReturnBadRequest() {
        InvalidTokenException ex = new InvalidTokenException("Token expired");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidToken(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Token expired", response.getBody().message());
        assertEquals(400, response.getBody().status());
    }

    @Test
    void handleUnauthorized_ShouldReturnUnauthorized() {
        UnauthorizedException ex = new UnauthorizedException("Not authenticated");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleUnauthorized(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Not authenticated", response.getBody().message());
        assertEquals(401, response.getBody().status());
    }

    @Test
    void handleBadRequest_ShouldReturnInvalidRequest() {
        InvalidRequestException ex = new InvalidRequestException("Invalid input");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidRequest(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid input", response.getBody().message());
        assertEquals(400, response.getBody().status());
    }

    @Test
    void handleConflict_ShouldReturnConflict() {
        ConflictException ex = new ConflictException("Resource conflict");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleConflict(ex, request);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Resource conflict", response.getBody().message());
        assertEquals(409, response.getBody().status());
    }

    @Test
    void handleInvalidTwoFactorCode_ShouldReturnUnauthorized() {
        InvalidTwoFactorCodeException ex = new InvalidTwoFactorCodeException("Invalid 2FA code");
        ResponseEntity<ErrorResponse> response = exceptionHandler.handleInvalidTwoFactorCode(ex, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Invalid 2FA code", response.getBody().message());
        assertEquals(401, response.getBody().status());
    }

    @Test
    void handleValidationExceptions_ShouldReturnValidationErrors() {
        // Mock validation errors
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);

        FieldError fieldError1 = new FieldError("object", "email", "Invalid email format");
        FieldError fieldError2 = new FieldError("object", "password", "Password too weak");

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getAllErrors()).thenReturn(List.of(fieldError1, fieldError2));

        ResponseEntity<ValidationErrorResponse> response = exceptionHandler.handleValidationExceptions(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().status());
        assertEquals("Validation failed", response.getBody().message());
        assertNotNull(response.getBody().errors());
        assertEquals(2, response.getBody().errors().size());
        assertTrue(response.getBody().errors().containsKey("email"));
        assertTrue(response.getBody().errors().containsKey("password"));
    }
}