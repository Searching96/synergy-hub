package com.synergyhub.common.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * Utility class for validation operations
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$"
    );

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^\\+?[1-9]\\d{9,14}$"
    );

    private ValidationUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Validate email format
     */
    public static boolean isValidEmail(String email) {
        return StringUtils.isNotBlank(email) && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate password strength
     * At least 8 characters, contains letters and numbers
     */
    public static boolean isValidPassword(String password) {
        return StringUtils.isNotBlank(password) && PASSWORD_PATTERN.matcher(password).matches();
    }

    /**
     * Validate phone number format (E.164)
     * Accepts 10-15 digits optionally starting with +
     */
    public static boolean isValidPhone(String phone) {
        return StringUtils.isNotBlank(phone) && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Check if string is null or blank
     */
    public static boolean isBlank(String str) {
        return StringUtils.isBlank(str);
    }

    /**
     * Check if string is not null and not blank
     */
    public static boolean isNotBlank(String str) {
        return StringUtils.isNotBlank(str);
    }

    /**
     * Validate string length
     */
    public static boolean isValidLength(String str, int min, int max) {
        if (str == null) {
            return false;
        }
        int length = str.length();
        return length >= min && length <= max;
    }
}