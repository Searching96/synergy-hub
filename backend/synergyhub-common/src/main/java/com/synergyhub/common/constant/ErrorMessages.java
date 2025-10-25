package com.synergyhub.common.constant;

/**
 * Standardized error messages
 */
public class ErrorMessages {

    private ErrorMessages() {
        throw new UnsupportedOperationException("Constants class");
    }

    // Generic Error Messages
    public static final String RESOURCE_NOT_FOUND = "Resource not found";
    public static final String INTERNAL_SERVER_ERROR = "An unexpected error occurred";
    public static final String BAD_REQUEST = "Invalid request";
    public static final String UNAUTHORIZED = "Unauthorized access";
    public static final String FORBIDDEN = "Access denied";

    // Authentication Error Messages
    public static final String INVALID_CREDENTIALS = "Invalid email or password";
    public static final String ACCOUNT_LOCKED = "Account is locked. Please try again later.";
    public static final String ACCOUNT_DISABLED = "Account is disabled";
    public static final String EMAIL_NOT_VERIFIED = "Email not verified";
    public static final String EMAIL_ALREADY_EXISTS = "Email already registered";
    public static final String INVALID_TOKEN = "Invalid or expired token";
    public static final String INVALID_2FA_CODE = "Invalid 2FA code";
    public static final String TOKEN_EXPIRED = "Token has expired";

    // User Error Messages
    public static final String USER_NOT_FOUND = "User not found";
    public static final String ORGANIZATION_NOT_FOUND = "Organization not found";
    public static final String ROLE_NOT_FOUND = "Role not found";
    public static final String PERMISSION_NOT_FOUND = "Permission not found";

    // Project Error Messages
    public static final String PROJECT_NOT_FOUND = "Project not found";
    public static final String SPRINT_NOT_FOUND = "Sprint not found";
    public static final String TASK_NOT_FOUND = "Task not found";

    // Resource Error Messages
    public static final String RESOURCE_NOT_AVAILABLE = "Resource not available";
    public static final String BOOKING_CONFLICT = "Booking time conflicts with existing booking";

    // Validation Error Messages
    public static final String INVALID_EMAIL_FORMAT = "Invalid email format";
    public static final String WEAK_PASSWORD = "Password must be at least 8 characters and contain letters and numbers";
    public static final String INVALID_PHONE_FORMAT = "Invalid phone number format";
    public static final String FIELD_REQUIRED = "This field is required";
    public static final String INVALID_DATE_RANGE = "Invalid date range";
}