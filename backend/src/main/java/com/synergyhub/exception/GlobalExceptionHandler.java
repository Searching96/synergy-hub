package com.synergyhub.exception;

import com.synergyhub.dto.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleResourceNotFoundException(
            ResourceNotFoundException ex, WebRequest request) {
        log.error("Resource not found: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Resource not found")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadRequestException(
            BadRequestException ex, WebRequest request) {
        log.error("Bad request: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Bad request")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(ContextMissingException.class)
    public ResponseEntity<ApiResponse<Object>> handleContextMissingException(
            ContextMissingException ex, WebRequest request) {
        log.error("Organization context missing: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Organization context required")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedException(
            UnauthorizedException ex, WebRequest request) {
        log.error("Unauthorized: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Unauthorized")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(SprintNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleSprintNotFoundException(
            SprintNotFoundException ex, WebRequest request) {
        log.error("Sprint not found: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Sprint not found")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(TaskNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleTaskNotFoundException(
            TaskNotFoundException ex, WebRequest request) {
        log.error("Task not found: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Task not found")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidSprintStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidSprintStateException(
            InvalidSprintStateException ex, WebRequest request) {
        log.error("Invalid sprint state: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Invalid sprint state")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(InvalidTaskStateException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidTaskStateException(
            InvalidTaskStateException ex, WebRequest request) {
        log.error("Invalid task state: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Invalid task state")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SprintAlreadyActiveException.class)
    public ResponseEntity<ApiResponse<Object>> handleSprintAlreadyActiveException(
            SprintAlreadyActiveException ex, WebRequest request) {
        log.error("Sprint already active: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Sprint already active")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(TaskAssignmentException.class)
    public ResponseEntity<ApiResponse<Object>> handleTaskAssignmentException(
            TaskAssignmentException ex, WebRequest request) {
        log.error("Task assignment error: {}", ex.getMessage());

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Task assignment error")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccountLockedException(
            AccountLockedException ex, WebRequest request) {
        log.error("Account locked: {}", ex.getMessage());
        
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("message", ex.getMessage());
        errorDetails.put("lockedUntil", ex.getLockUntil());
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Account locked")
                .data(errorDetails)
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidTokenException(
            InvalidTokenException ex, WebRequest request) {
        log.error("Invalid token: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Invalid or expired token")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(TwoFactorAuthenticationException.class)
    public ResponseEntity<ApiResponse<Object>> handleTwoFactorAuthenticationException(
            TwoFactorAuthenticationException ex, WebRequest request) {
        log.error("2FA error: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Two-factor authentication error")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleEmailAlreadyExistsException(
            EmailAlreadyExistsException ex, WebRequest request) {
        log.error("Email already exists: {}", ex.getMessage());
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Email already exists")
                .error(ex.getMessage())
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ApiResponse<Object>> handleBadCredentialsException(
            BadCredentialsException ex, WebRequest request) {
        log.error("Bad credentials: {}", ex.getMessage());

        String detail = ex.getMessage();
        String message = "Invalid credentials";
        String error = "Email or password is incorrect";

        if (detail != null) {
            if (detail.toLowerCase().contains("email not verified")) {
                message = "Email not verified";
                error = detail;
            } else if (!detail.isBlank() && !detail.equalsIgnoreCase("bad credentials")) {
                error = detail;
            }
        }

        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(message)
                .error(error)
                .timestamp(LocalDateTime.now())
                .build();

        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }
    
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Object>> handleAccessDeniedException(
            AccessDeniedException ex, WebRequest request) {
        log.error("Access denied: {}", ex.getMessage());
        
        // Provide helpful error message with context about what permission is needed
        String errorMessage = ex.getMessage() != null && !ex.getMessage().isEmpty() 
            ? ex.getMessage() 
            : "You don't have permission to access this resource";
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message(errorMessage)  // Use exception message if available for specific context
                .error("Insufficient permissions. Contact your project administrator if you need access.")
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.FORBIDDEN);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        log.error("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Validation failed")
                .data(errors)
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Object>> handleConstraintViolationException(
            ConstraintViolationException ex) {
        log.error("Constraint violation: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getConstraintViolations().forEach(violation -> {
            String propertyPath = violation.getPropertyPath().toString();
            String message = violation.getMessage();
            errors.put(propertyPath, message);
        });
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Validation failed")
                .data(errors)
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Object>> handleGlobalException(
            Exception ex, WebRequest request) {
        log.error("Unexpected error: ", ex);
        
        ApiResponse<Object> response = ApiResponse.builder()
                .success(false)
                .message("Internal server error")
                .error("An unexpected error occurred. Please try again later.")
                .timestamp(LocalDateTime.now())
                .build();
        
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ProjectNotFoundException.class)
    public ResponseEntity<ApiResponse<Object>> handleProjectNotFound(
            ProjectNotFoundException ex) {
        log.error("Project not found: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error("Project not found", ex.getMessage()));
    }

    @ExceptionHandler(ProjectNameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Object>> handleProjectNameAlreadyExists(
            ProjectNameAlreadyExistsException ex) {
        log.error("Project name already exists: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ApiResponse.error("Project name already exists", ex.getMessage()));
    }

    @ExceptionHandler(UnauthorizedProjectAccessException.class)
    public ResponseEntity<ApiResponse<Object>> handleUnauthorizedProjectAccess(
            UnauthorizedProjectAccessException ex) {
        log.error("Unauthorized project access: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error("Unauthorized access", ex.getMessage()));
    }

    @ExceptionHandler(InvalidProjectMemberException.class)
    public ResponseEntity<ApiResponse<Object>> handleInvalidProjectMember(
            InvalidProjectMemberException ex) {
        log.error("Invalid project member operation: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Invalid project member operation", ex.getMessage()));
    }
}