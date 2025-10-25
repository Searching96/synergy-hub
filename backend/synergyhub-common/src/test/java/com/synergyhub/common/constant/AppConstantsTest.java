package com.synergyhub.common.constant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class AppConstantsTest {

    @Test
    void paginationConstants_ShouldHaveCorrectValues() {
        assertEquals(20, AppConstants.DEFAULT_PAGE_SIZE);
        assertEquals(100, AppConstants.MAX_PAGE_SIZE);
        assertEquals("createdAt", AppConstants.DEFAULT_SORT_BY);
        assertEquals("DESC", AppConstants.DEFAULT_SORT_DIRECTION);
    }

    @Test
    void securityConstants_ShouldHaveCorrectValues() {
        assertEquals(5, AppConstants.MAX_LOGIN_ATTEMPTS);
        assertEquals(30, AppConstants.LOCKOUT_DURATION_MINUTES);
        assertEquals(15, AppConstants.PASSWORD_RESET_TOKEN_EXPIRY_MINUTES);
        assertEquals(24, AppConstants.EMAIL_VERIFICATION_TOKEN_EXPIRY_HOURS);
        assertEquals(24, AppConstants.JWT_EXPIRATION_HOURS);
    }

    @Test
    void validationConstants_ShouldHaveCorrectValues() {
        assertEquals(8, AppConstants.MIN_PASSWORD_LENGTH);
        assertEquals(100, AppConstants.MAX_PASSWORD_LENGTH);
        assertEquals(2, AppConstants.MIN_NAME_LENGTH);
        assertEquals(100, AppConstants.MAX_NAME_LENGTH);
        assertEquals(100, AppConstants.MAX_EMAIL_LENGTH);
        assertEquals(5000, AppConstants.MAX_DESCRIPTION_LENGTH);
    }

    @Test
    void dateTimeFormats_ShouldBeCorrect() {
        assertEquals("yyyy-MM-dd", AppConstants.DATE_FORMAT);
        assertEquals("yyyy-MM-dd HH:mm:ss", AppConstants.DATETIME_FORMAT);
        assertEquals("HH:mm:ss", AppConstants.TIME_FORMAT);

        assertNotNull(AppConstants.DATE_FORMAT);
        assertNotNull(AppConstants.DATETIME_FORMAT);
        assertNotNull(AppConstants.TIME_FORMAT);
    }

    @Test
    void fileUploadConstants_ShouldBeCorrect() {
        assertEquals(10 * 1024 * 1024, AppConstants.MAX_FILE_SIZE);
        assertArrayEquals(new String[]{"image/jpeg", "image/png", "image/gif"},
                AppConstants.ALLOWED_IMAGE_TYPES);

        assertTrue(AppConstants.MAX_FILE_SIZE > 0);
        assertEquals(3, AppConstants.ALLOWED_IMAGE_TYPES.length);
    }

    @Test
    void cacheNames_ShouldBeCorrect() {
        assertEquals("users", AppConstants.CACHE_USER);
        assertEquals("roles", AppConstants.CACHE_ROLE);
        assertEquals("permissions", AppConstants.CACHE_PERMISSION);

        assertNotNull(AppConstants.CACHE_USER);
        assertNotNull(AppConstants.CACHE_ROLE);
        assertNotNull(AppConstants.CACHE_PERMISSION);
    }

    @Test
    void headerConstants_ShouldBeCorrect() {
        assertEquals("Authorization", AppConstants.HEADER_AUTHORIZATION);
        assertEquals("Bearer ", AppConstants.HEADER_BEARER_PREFIX);

        assertNotNull(AppConstants.HEADER_AUTHORIZATION);
        assertNotNull(AppConstants.HEADER_BEARER_PREFIX);
        assertTrue(AppConstants.HEADER_BEARER_PREFIX.endsWith(" "));
    }

    @Test
    void constructor_ShouldThrowException() throws NoSuchMethodException {
        Constructor<AppConstants> constructor = AppConstants.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
        assertEquals("Constants class", exception.getCause().getMessage());
    }
}