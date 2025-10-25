package com.synergyhub.auth.util;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TOTPUtilTest {

    @Test
    void generateSecret_ShouldReturnNonEmptyString() {
        // Act
        String secret = TOTPUtil.generateSecret();

        // Assert
        assertNotNull(secret);
        assertFalse(secret.isEmpty());
        assertTrue(secret.length() > 10);
    }

    @Test
    void generateSecret_ShouldReturnDifferentSecrets() {
        // Act
        String secret1 = TOTPUtil.generateSecret();
        String secret2 = TOTPUtil.generateSecret();

        // Assert
        assertNotEquals(secret1, secret2);
    }

    @Test
    void verifyCode_WithNullSecret_ShouldReturnFalse() {
        // Act
        boolean result = TOTPUtil.verifyCode(null, "123456");

        // Assert
        assertFalse(result);
    }

    @Test
    void verifyCode_WithNullCode_ShouldReturnFalse() {
        // Act
        boolean result = TOTPUtil.verifyCode("secret", null);

        // Assert
        assertFalse(result);
    }

    @Test
    void generateQRCodeUrl_ShouldReturnDataUrl() {
        // Arrange
        String email = "test@example.com";
        String secret = TOTPUtil.generateSecret();
        String issuer = "SynergyHub";

        // Act
        String qrCodeUrl = TOTPUtil.generateQRCodeUrl(email, secret, issuer);

        // Assert
        assertNotNull(qrCodeUrl);
        assertTrue(qrCodeUrl.startsWith("data:image/png;base64,"));
    }

    @Test
    void generateBackupCodes_ShouldReturn10Codes() {
        // Act
        List<String> codes = TOTPUtil.generateBackupCodes();

        // Assert
        assertEquals(10, codes.size());
    }

    @Test
    void generateBackupCodes_ShouldReturnUniqueCodes() {
        // Act
        List<String> codes = TOTPUtil.generateBackupCodes();

        // Assert
        assertEquals(10, codes.stream().distinct().count());
    }

    @Test
    void generateBackupCodes_ShouldReturn8DigitCodes() {
        // Act
        List<String> codes = TOTPUtil.generateBackupCodes();

        // Assert
        codes.forEach(code -> {
            assertEquals(8, code.length());
            assertTrue(code.matches("\\d{8}"));
        });
    }

    @Test
    void backupCodesToJson_ShouldConvertCorrectly() {
        // Arrange
        List<String> codes = List.of("12345678", "87654321");

        // Act
        String json = TOTPUtil.backupCodesToJson(codes);

        // Assert
        assertNotNull(json);
        assertTrue(json.contains("12345678"));
        assertTrue(json.contains("87654321"));
    }

    @Test
    void jsonToBackupCodes_ShouldConvertCorrectly() {
        // Arrange
        String json = "[\"12345678\",\"87654321\"]";

        // Act
        List<String> codes = TOTPUtil.jsonToBackupCodes(json);

        // Assert
        assertEquals(2, codes.size());
        assertTrue(codes.contains("12345678"));
        assertTrue(codes.contains("87654321"));
    }

    @Test
    void jsonToBackupCodes_WithNullJson_ShouldReturnEmptyList() {
        // Act
        List<String> codes = TOTPUtil.jsonToBackupCodes(null);

        // Assert
        assertTrue(codes.isEmpty());
    }

    @Test
    void jsonToBackupCodes_WithEmptyJson_ShouldReturnEmptyList() {
        // Act
        List<String> codes = TOTPUtil.jsonToBackupCodes("");

        // Assert
        assertTrue(codes.isEmpty());
    }

    @Test
    void verifyBackupCode_WithValidCode_ShouldReturnTrue() {
        // Arrange
        String json = "[\"12345678\",\"87654321\"]";

        // Act
        boolean result = TOTPUtil.verifyBackupCode(json, "12345678");

        // Assert
        assertTrue(result);
    }

    @Test
    void verifyBackupCode_WithInvalidCode_ShouldReturnFalse() {
        // Arrange
        String json = "[\"12345678\",\"87654321\"]";

        // Act
        boolean result = TOTPUtil.verifyBackupCode(json, "00000000");

        // Assert
        assertFalse(result);
    }

    @Test
    void verifyBackupCode_WithNullJson_ShouldReturnFalse() {
        // Act
        boolean result = TOTPUtil.verifyBackupCode(null, "12345678");

        // Assert
        assertFalse(result);
    }

    @Test
    void removeBackupCode_ShouldRemoveCodeFromList() {
        // Arrange
        String json = "[\"12345678\",\"87654321\",\"11111111\"]";

        // Act
        String result = TOTPUtil.removeBackupCode(json, "87654321");
        List<String> codes = TOTPUtil.jsonToBackupCodes(result);

        // Assert
        assertEquals(2, codes.size());
        assertFalse(codes.contains("87654321"));
        assertTrue(codes.contains("12345678"));
        assertTrue(codes.contains("11111111"));
    }

    @Test
    void roundTripConversion_ShouldMaintainData() {
        // Arrange
        List<String> originalCodes = TOTPUtil.generateBackupCodes();

        // Act
        String json = TOTPUtil.backupCodesToJson(originalCodes);
        List<String> restoredCodes = TOTPUtil.jsonToBackupCodes(json);

        // Assert
        assertEquals(originalCodes.size(), restoredCodes.size());
        assertTrue(restoredCodes.containsAll(originalCodes));
    }
}