package com.synergyhub.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilTest {

    @Test
    void isValidEmail_WithValidEmail_ShouldReturnTrue() {
        assertTrue(ValidationUtil.isValidEmail("test@example.com"));
        assertTrue(ValidationUtil.isValidEmail("user.name@domain.co.uk"));
        assertTrue(ValidationUtil.isValidEmail("user+tag@example.org"));
        assertTrue(ValidationUtil.isValidEmail("alice123@test.com"));
        assertTrue(ValidationUtil.isValidEmail("john_doe@company.net"));
    }

    @Test
    void isValidEmail_WithInvalidEmail_ShouldReturnFalse() {
        assertFalse(ValidationUtil.isValidEmail("invalid"));
        assertFalse(ValidationUtil.isValidEmail("@example.com"));
        assertFalse(ValidationUtil.isValidEmail("user@"));
        assertFalse(ValidationUtil.isValidEmail("user@.com"));
        assertFalse(ValidationUtil.isValidEmail("user name@example.com"));
    }

    @Test
    void isValidEmail_WithNullOrEmpty_ShouldReturnFalse() {
        String nullEmail = null;
        assertFalse(ValidationUtil.isValidEmail(nullEmail));
        assertFalse(ValidationUtil.isValidEmail(""));
        assertFalse(ValidationUtil.isValidEmail("   "));
    }

    @Test
    void isValidPassword_WithValidPassword_ShouldReturnTrue() {
        assertTrue(ValidationUtil.isValidPassword("Password123"));
        assertTrue(ValidationUtil.isValidPassword("Test1234"));
        assertTrue(ValidationUtil.isValidPassword("MyP@ssw0rd"));
        assertTrue(ValidationUtil.isValidPassword("Secure99"));
        assertTrue(ValidationUtil.isValidPassword("Abcdefgh1"));
    }

    @Test
    void isValidPassword_WithInvalidPassword_ShouldReturnFalse() {
        assertFalse(ValidationUtil.isValidPassword("short"));
        assertFalse(ValidationUtil.isValidPassword("nodigitshere"));
        assertFalse(ValidationUtil.isValidPassword("12345678"));
        assertFalse(ValidationUtil.isValidPassword(""));
        assertFalse(ValidationUtil.isValidPassword("ONLYUPPERCASE123"));
        assertFalse(ValidationUtil.isValidPassword("onlylowercase123"));
    }

    @Test
    void isValidPassword_WithNullOrEmpty_ShouldReturnFalse() {
        String nullPassword = null;
        assertFalse(ValidationUtil.isValidPassword(nullPassword));
        assertFalse(ValidationUtil.isValidPassword(""));
        assertFalse(ValidationUtil.isValidPassword("   "));
    }

    @Test
    void isValidPhone_WithValidPhone_ShouldReturnTrue() {
        assertTrue(ValidationUtil.isValidPhone("+1234567890"));
        assertTrue(ValidationUtil.isValidPhone("1234567890"));
        assertTrue(ValidationUtil.isValidPhone("+84987654321"));
        assertTrue(ValidationUtil.isValidPhone("+442071234567"));
        assertTrue(ValidationUtil.isValidPhone("9876543210"));
    }

    @Test
    void isValidPhone_WithInvalidPhone_ShouldReturnFalse() {
        assertFalse(ValidationUtil.isValidPhone("123"));
        assertFalse(ValidationUtil.isValidPhone("abc"));
        assertFalse(ValidationUtil.isValidPhone("+abc123"));
        assertFalse(ValidationUtil.isValidPhone(""));
    }

    @Test
    void isValidPhone_WithNullOrEmpty_ShouldReturnFalse() {
        String nullPhone = null;
        assertFalse(ValidationUtil.isValidPhone(nullPhone));
        assertFalse(ValidationUtil.isValidPhone(""));
        assertFalse(ValidationUtil.isValidPhone("   "));
    }

    @Test
    void isBlank_ShouldWorkCorrectly() {
        String nullString = null;
        assertTrue(ValidationUtil.isBlank(nullString));
        assertTrue(ValidationUtil.isBlank(""));
        assertTrue(ValidationUtil.isBlank("   "));
        assertTrue(ValidationUtil.isBlank("\t\n"));
        assertFalse(ValidationUtil.isBlank("text"));
        assertFalse(ValidationUtil.isBlank("  text  "));
    }

    @Test
    void isNotBlank_ShouldWorkCorrectly() {
        String nullString = null;
        assertFalse(ValidationUtil.isNotBlank(nullString));
        assertFalse(ValidationUtil.isNotBlank(""));
        assertFalse(ValidationUtil.isNotBlank("   "));
        assertFalse(ValidationUtil.isNotBlank("\t\n"));
        assertTrue(ValidationUtil.isNotBlank("text"));
        assertTrue(ValidationUtil.isNotBlank("  text  "));
    }

    @Test
    void isValidLength_ShouldValidateCorrectly() {
        assertTrue(ValidationUtil.isValidLength("test", 2, 10));
        assertTrue(ValidationUtil.isValidLength("ab", 2, 10));
        assertTrue(ValidationUtil.isValidLength("1234567890", 2, 10));
        assertTrue(ValidationUtil.isValidLength("hello", 2, 10));
        assertFalse(ValidationUtil.isValidLength("a", 2, 10));
        assertFalse(ValidationUtil.isValidLength("12345678901", 2, 10));
    }

    @Test
    void isValidLength_WithNull_ShouldReturnFalse() {
        String nullString = null;
        assertFalse(ValidationUtil.isValidLength(nullString, 2, 10));
    }

    @Test
    void isValidLength_WithExactBoundaries_ShouldReturnTrue() {
        assertTrue(ValidationUtil.isValidLength("ab", 2, 2));
        assertTrue(ValidationUtil.isValidLength("hello", 5, 5));
        assertTrue(ValidationUtil.isValidLength("test", 4, 4));
    }
}