package com.synergyhub.common.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for date and time operations
 */
public class DateTimeUtil {

    private static final String DEFAULT_DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final DateTimeFormatter DEFAULT_FORMATTER = DateTimeFormatter.ofPattern(DEFAULT_DATETIME_FORMAT);

    private DateTimeUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Get current date time
     */
    public static LocalDateTime now() {
        return LocalDateTime.now();
    }

    /**
     * Format LocalDateTime to string with default format
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DEFAULT_FORMATTER);
    }

    /**
     * Format LocalDateTime to string with custom format
     */
    public static String format(LocalDateTime dateTime, String pattern) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * Parse string to LocalDateTime with default format
     */
    public static LocalDateTime parse(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        return LocalDateTime.parse(dateTimeStr, DEFAULT_FORMATTER);
    }

    /**
     * Check if dateTime is in the past
     */
    public static boolean isPast(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isBefore(LocalDateTime.now());
    }

    /**
     * Check if dateTime is in the future
     */
    public static boolean isFuture(LocalDateTime dateTime) {
        return dateTime != null && dateTime.isAfter(LocalDateTime.now());
    }

    /**
     * Add minutes to current time
     */
    public static LocalDateTime nowPlusMinutes(long minutes) {
        return LocalDateTime.now().plusMinutes(minutes);
    }

    /**
     * Add hours to current time
     */
    public static LocalDateTime nowPlusHours(long hours) {
        return LocalDateTime.now().plusHours(hours);
    }

    /**
     * Add days to current time
     */
    public static LocalDateTime nowPlusDays(long days) {
        return LocalDateTime.now().plusDays(days);
    }
}