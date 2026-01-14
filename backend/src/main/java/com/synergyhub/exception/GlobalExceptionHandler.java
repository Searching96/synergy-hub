package com.synergyhub.exception;

import com.synergyhub.dto.response.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error("NOT_FOUND", ex.getMessage()), HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(OrganizationNameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleOrganizationNameAlreadyExistsException(OrganizationNameAlreadyExistsException ex, WebRequest request) {
        log.warn("Organization name conflict: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error("CONFLICT", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(InvalidInviteCodeException.class)
    public ResponseEntity<ApiResponse<Void>> handleInvalidInviteCodeException(InvalidInviteCodeException ex, WebRequest request) {
        log.warn("Invalid invite code: {}", ex.getMessage());
        return new ResponseEntity<>(ApiResponse.error("INVALID_INVITE", ex.getMessage()), HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(UserAlreadyInOrganizationException.class)
    public ResponseEntity<ApiResponse<Void>> handleUserAlreadyInOrganizationException(UserAlreadyInOrganizationException ex, WebRequest request) {
         log.info("User already in organization: {}", ex.getMessage());
         return new ResponseEntity<>(ApiResponse.error("ALREADY_MEMBER", ex.getMessage()), HttpStatus.CONFLICT);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.debug("Validation failed: {}", errors);
        return new ResponseEntity<>(ApiResponse.error("VALIDATION_FAILED", "Validation failed", errors), HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiResponse<Void>> handleResponseStatusException(ResponseStatusException ex) {
         log.error("ResponseStatusException: {}", ex.getReason());
         return new ResponseEntity<>(ApiResponse.error("ERROR", ex.getReason()), ex.getStatusCode());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGlobalException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        return new ResponseEntity<>(ApiResponse.error("INTERNAL_ERROR", "An unexpected error occurred"), HttpStatus.INTERNAL_SERVER_ERROR);
    }
}