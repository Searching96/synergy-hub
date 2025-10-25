package com.synergyhub.auth.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordValidatorTest {

    private PasswordValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PasswordValidator();
        validator.initialize(null);
    }

    @Test
    void isValid_WithValidPassword_ShouldReturnTrue() {
        assertTrue(validator.isValid("Password123", null));
        assertTrue(validator.isValid("Test1234", null));
        assertTrue(validator.isValid("MyP@ssw0rd", null));
        assertTrue(validator.isValid("Secure99", null));
    }

    @Test
    void isValid_WithInvalidPassword_ShouldReturnFalse() {
        assertFalse(validator.isValid("short", null));
        assertFalse(validator.isValid("nodigitshere", null));
        assertFalse(validator.isValid("12345678", null));
        assertFalse(validator.isValid("ONLYUPPERCASE123", null));
    }

    @Test
    void isValid_WithNullPassword_ShouldReturnFalse() {
        assertFalse(validator.isValid(null, null));
    }

    @Test
    void isValid_WithEmptyPassword_ShouldReturnFalse() {
        assertFalse(validator.isValid("", null));
        assertFalse(validator.isValid("   ", null));
    }

    @Test
    void isValid_WithMinimumLength_ShouldReturnTrue() {
        assertTrue(validator.isValid("Test1234", null)); // exactly 8 chars
    }

    @Test
    void isValid_WithSpecialCharacters_ShouldReturnTrue() {
        assertTrue(validator.isValid("P@ssw0rd!", null));
        assertTrue(validator.isValid("Test#123$", null));
    }
}