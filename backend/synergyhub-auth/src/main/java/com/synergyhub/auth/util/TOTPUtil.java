package com.synergyhub.auth.util;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.exceptions.QrGenerationException;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Utility class for TOTP (Time-based One-Time Password) operations
 */
public class TOTPUtil {

    private static final TimeProvider timeProvider = new SystemTimeProvider();
    private static final CodeGenerator codeGenerator = new DefaultCodeGenerator();
    private static final CodeVerifier verifier = new DefaultCodeVerifier(codeGenerator, timeProvider);
    private static final Gson gson = new Gson();

    private TOTPUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Generate a new TOTP secret
     */
    public static String generateSecret() {
        return new DefaultSecretGenerator().generate();
    }

    /**
     * Verify TOTP code
     */
    public static boolean verifyCode(String secret, String code) {
        if (secret == null || code == null) {
            return false;
        }
        return verifier.isValidCode(secret, code);
    }

    /**
     * Generate QR code URL for authenticator apps
     */
    public static String generateQRCodeUrl(String email, String secret, String issuer) {
        QrData data = new QrData.Builder()
            .label(email)
            .secret(secret)
            .issuer(issuer)
            .algorithm(HashingAlgorithm.SHA1)
            .digits(6)
            .period(30)
            .build();

        QrGenerator generator = new ZxingPngQrGenerator();
        try {
            byte[] imageData = generator.generate(data);
            String mimeType = generator.getImageMimeType();
            return "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(imageData);
        } catch (QrGenerationException e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }

    /**
     * Generate backup codes
     */
    public static List<String> generateBackupCodes() {
        List<String> codes = new ArrayList<>();
        SecureRandom random = new SecureRandom();
        
        for (int i = 0; i < 10; i++) {
            String code = String.format("%08d", random.nextInt(100000000));
            codes.add(code);
        }
        
        return codes;
    }

    /**
     * Convert backup codes to JSON
     */
    public static String backupCodesToJson(List<String> codes) {
        return gson.toJson(codes);
    }

    /**
     * Convert JSON to backup codes
     */
    public static List<String> jsonToBackupCodes(String json) {
        if (json == null || json.trim().isEmpty()) {
            return new ArrayList<>();
        }
        return gson.fromJson(json, new TypeToken<List<String>>(){}.getType());
    }

    /**
     * Verify backup code
     */
    public static boolean verifyBackupCode(String backupCodesJson, String code) {
        if (backupCodesJson == null || code == null) {
            return false;
        }
        List<String> codes = jsonToBackupCodes(backupCodesJson);
        return codes.contains(code);
    }

    /**
     * Remove used backup code
     */
    public static String removeBackupCode(String backupCodesJson, String code) {
        List<String> codes = jsonToBackupCodes(backupCodesJson);
        codes.remove(code);
        return backupCodesToJson(codes);
    }
}