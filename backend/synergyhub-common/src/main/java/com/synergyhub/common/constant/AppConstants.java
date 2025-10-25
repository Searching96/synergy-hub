package com.synergyhub.common.constant;

/**
 * Application-wide constants
 */
public class AppConstants {

    private AppConstants() {
        throw new UnsupportedOperationException("Constants class");
    }

    // Pagination
    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;
    public static final String DEFAULT_SORT_BY = "createdAt";
    public static final String DEFAULT_SORT_DIRECTION = "DESC";

    // Date Time Formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    public static final String TIME_FORMAT = "HH:mm:ss";

    // Security
    public static final int MAX_LOGIN_ATTEMPTS = 5;
    public static final int LOCKOUT_DURATION_MINUTES = 30;
    public static final int PASSWORD_RESET_TOKEN_EXPIRY_MINUTES = 15;
    public static final int EMAIL_VERIFICATION_TOKEN_EXPIRY_HOURS = 24;
    public static final int JWT_EXPIRATION_HOURS = 24;

    // Validation
    public static final int MIN_PASSWORD_LENGTH = 8;
    public static final int MAX_PASSWORD_LENGTH = 100;
    public static final int MIN_NAME_LENGTH = 2;
    public static final int MAX_NAME_LENGTH = 100;
    public static final int MAX_EMAIL_LENGTH = 100;
    public static final int MAX_DESCRIPTION_LENGTH = 5000;

    // File Upload
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    public static final String[] ALLOWED_IMAGE_TYPES = {"image/jpeg", "image/png", "image/gif"};

    // Cache Names
    public static final String CACHE_USER = "users";
    public static final String CACHE_ROLE = "roles";
    public static final String CACHE_PERMISSION = "permissions";

    // HTTP Header Names
    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String HEADER_BEARER_PREFIX = "Bearer ";
}