package com.synergyhub.common.constant;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.*;

class ErrorMessagesTest {

    @Test
    void genericMessages_ShouldNotBeBlank() {
        assertNotNull(ErrorMessages.RESOURCE_NOT_FOUND);
        assertNotNull(ErrorMessages.INTERNAL_SERVER_ERROR);
        assertNotNull(ErrorMessages.BAD_REQUEST);
        assertNotNull(ErrorMessages.UNAUTHORIZED);
        assertNotNull(ErrorMessages.FORBIDDEN);

        assertFalse(ErrorMessages.RESOURCE_NOT_FOUND.isEmpty());
        assertFalse(ErrorMessages.INTERNAL_SERVER_ERROR.isEmpty());
        assertFalse(ErrorMessages.BAD_REQUEST.isEmpty());
        assertFalse(ErrorMessages.UNAUTHORIZED.isEmpty());
        assertFalse(ErrorMessages.FORBIDDEN.isEmpty());
    }

    @Test
    void authenticationMessages_ShouldNotBeBlank() {
        assertNotNull(ErrorMessages.INVALID_CREDENTIALS);
        assertNotNull(ErrorMessages.ACCOUNT_LOCKED);
        assertNotNull(ErrorMessages.EMAIL_ALREADY_EXISTS);
        assertNotNull(ErrorMessages.INVALID_TOKEN);
        assertNotNull(ErrorMessages.INVALID_2FA_CODE);
        assertNotNull(ErrorMessages.TOKEN_EXPIRED);

        assertFalse(ErrorMessages.INVALID_CREDENTIALS.isEmpty());
        assertFalse(ErrorMessages.ACCOUNT_LOCKED.isEmpty());
    }

    @Test
    void userMessages_ShouldNotBeBlank() {
        assertNotNull(ErrorMessages.USER_NOT_FOUND);
        assertNotNull(ErrorMessages.ORGANIZATION_NOT_FOUND);
        assertNotNull(ErrorMessages.ROLE_NOT_FOUND);
        assertNotNull(ErrorMessages.PERMISSION_NOT_FOUND);

        assertFalse(ErrorMessages.USER_NOT_FOUND.isEmpty());
        assertFalse(ErrorMessages.ORGANIZATION_NOT_FOUND.isEmpty());
    }

    @Test
    void projectMessages_ShouldNotBeBlank() {
        assertNotNull(ErrorMessages.PROJECT_NOT_FOUND);
        assertNotNull(ErrorMessages.SPRINT_NOT_FOUND);
        assertNotNull(ErrorMessages.TASK_NOT_FOUND);

        assertFalse(ErrorMessages.PROJECT_NOT_FOUND.isEmpty());
        assertFalse(ErrorMessages.SPRINT_NOT_FOUND.isEmpty());
        assertFalse(ErrorMessages.TASK_NOT_FOUND.isEmpty());
    }

    @Test
    void resourceMessages_ShouldNotBeBlank() {
        assertNotNull(ErrorMessages.RESOURCE_NOT_AVAILABLE);
        assertNotNull(ErrorMessages.BOOKING_CONFLICT);

        assertFalse(ErrorMessages.RESOURCE_NOT_AVAILABLE.isEmpty());
        assertFalse(ErrorMessages.BOOKING_CONFLICT.isEmpty());
    }

    @Test
    void validationMessages_ShouldNotBeBlank() {
        assertNotNull(ErrorMessages.INVALID_EMAIL_FORMAT);
        assertNotNull(ErrorMessages.WEAK_PASSWORD);
        assertNotNull(ErrorMessages.INVALID_PHONE_FORMAT);
        assertNotNull(ErrorMessages.FIELD_REQUIRED);
        assertNotNull(ErrorMessages.INVALID_DATE_RANGE);

        assertFalse(ErrorMessages.INVALID_EMAIL_FORMAT.isEmpty());
        assertFalse(ErrorMessages.WEAK_PASSWORD.isEmpty());
    }

    @Test
    void constructor_ShouldThrowException() throws NoSuchMethodException {
        Constructor<ErrorMessages> constructor = ErrorMessages.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        InvocationTargetException exception = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, exception.getCause());
        assertEquals("Constants class", exception.getCause().getMessage());
    }
}