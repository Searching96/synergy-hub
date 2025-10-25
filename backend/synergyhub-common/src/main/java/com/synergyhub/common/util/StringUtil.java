package com.synergyhub.common.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class for string operations
 */
public class StringUtil {

    private StringUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Capitalize first letter
     */
    public static String capitalizeFirst(String str) {
        if (StringUtils.isBlank(str)) {
            return str;
        }
        return StringUtils.capitalize(str);
    }

    /**
     * Convert to lowercase and trim
     */
    public static String normalizeLowerCase(String str) {
        if (str == null) {
            return null;
        }
        return str.trim().toLowerCase();
    }

    /**
     * Convert to uppercase and trim
     */
    public static String normalizeUpperCase(String str) {
        if (str == null) {
            return null;
        }
        return str.trim().toUpperCase();
    }

    /**
     * Remove special characters, keeping only alphanumeric
     */
    public static String removeSpecialCharacters(String str) {
        if (str == null) {
            return null;
        }
        return str.replaceAll("[^a-zA-Z0-9]", "");
    }

    /**
     * Truncate string to max length with ellipsis
     */
    public static String truncate(String str, int maxLength) {
        if (str == null) {
            return null;
        }
        if (str.length() <= maxLength) {
            return str;
        }
        if (maxLength < 3) {
            return str.substring(0, maxLength);
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Mask email (show only first 3 chars and domain)
     * Example: john.doe@example.com -> joh***@example.com
     */
    public static String maskEmail(String email) {
        if (!ValidationUtil.isValidEmail(email)) {
            return email;
        }
        String[] parts = email.split("@");
        if (parts[0].length() <= 3) {
            return "***@" + parts[1];
        }
        return parts[0].substring(0, 3) + "***@" + parts[1];
    }
}