package com.synergyhub.common.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class StringUtilTest {

    @Test
    void capitalizeFirst_ShouldCapitalizeCorrectly() {
        assertEquals("Test", StringUtil.capitalizeFirst("test"));
        assertEquals("Test", StringUtil.capitalizeFirst("Test"));
        assertEquals("T", StringUtil.capitalizeFirst("t"));
    }

    @Test
    void capitalizeFirst_WithNullOrEmpty_ShouldReturnAsIs() {
        assertNull(StringUtil.capitalizeFirst(null));
        assertEquals("", StringUtil.capitalizeFirst(""));
        assertEquals("   ", StringUtil.capitalizeFirst("   "));
    }

    @Test
    void normalizeLowerCase_ShouldNormalizeCorrectly() {
        assertEquals("test", StringUtil.normalizeLowerCase("TEST"));
        assertEquals("test", StringUtil.normalizeLowerCase("  test  "));
        assertEquals("test@example.com", StringUtil.normalizeLowerCase("TEST@EXAMPLE.COM"));
        assertEquals("hello world", StringUtil.normalizeLowerCase("  HELLO WORLD  "));
    }

    @Test
    void normalizeLowerCase_WithNull_ShouldReturnNull() {
        String result = StringUtil.normalizeLowerCase(null);
        assertNull(result);
    }

    @Test
    void normalizeUpperCase_ShouldNormalizeCorrectly() {
        assertEquals("TEST", StringUtil.normalizeUpperCase("test"));
        assertEquals("TEST", StringUtil.normalizeUpperCase("  test  "));
        assertEquals("HELLO WORLD", StringUtil.normalizeUpperCase("  hello world  "));
    }

    @Test
    void normalizeUpperCase_WithNull_ShouldReturnNull() {
        String result = StringUtil.normalizeUpperCase(null);
        assertNull(result);
    }

    @Test
    void removeSpecialCharacters_ShouldRemoveCorrectly() {
        assertEquals("test123", StringUtil.removeSpecialCharacters("test@123!"));
        assertEquals("HelloWorld", StringUtil.removeSpecialCharacters("Hello-World!"));
        assertEquals("", StringUtil.removeSpecialCharacters("!@#$%"));
        assertEquals("abc123XYZ", StringUtil.removeSpecialCharacters("abc@123#XYZ!"));
    }

    @Test
    void removeSpecialCharacters_WithNull_ShouldReturnNull() {
        String input = null;
        String result = StringUtil.removeSpecialCharacters(input);
        assertNull(result);
    }

    @Test
    void truncate_ShouldTruncateCorrectly() {
        assertEquals("Hello...", StringUtil.truncate("Hello World", 8));
        assertEquals("This is...", StringUtil.truncate("This is a long text", 10));
        assertEquals("1234567...", StringUtil.truncate("1234567890123", 10));
    }

    @Test
    void truncate_WithShortString_ShouldReturnOriginal() {
        assertEquals("Test", StringUtil.truncate("Test", 10));
        assertEquals("Short", StringUtil.truncate("Short", 20));
        assertEquals("1234567890", StringUtil.truncate("1234567890", 10));
    }

    @Test
    void truncate_WithNull_ShouldReturnNull() {
        String input = null;
        String result = StringUtil.truncate(input, 10);
        assertNull(result);
    }

    @Test
    void maskEmail_ShouldMaskCorrectly() {
        assertEquals("joh***@example.com", StringUtil.maskEmail("john@example.com"));
        assertEquals("joh***@example.com", StringUtil.maskEmail("john.doe@example.com"));
        assertEquals("ali***@test.org", StringUtil.maskEmail("alice@test.org"));
    }

    @Test
    void maskEmail_WithShortUsername_ShouldMaskCompletely() {
        assertEquals("***@test.com", StringUtil.maskEmail("ab@test.com"));
        assertEquals("***@example.org", StringUtil.maskEmail("a@example.org"));
    }

    @Test
    void maskEmail_WithInvalidEmail_ShouldReturnAsIs() {
        assertEquals("invalid", StringUtil.maskEmail("invalid"));
        assertEquals("not-an-email", StringUtil.maskEmail("not-an-email"));
        assertEquals("@missing.com", StringUtil.maskEmail("@missing.com"));
    }
}