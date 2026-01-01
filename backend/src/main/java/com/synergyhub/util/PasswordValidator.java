package com.synergyhub.util;

import org.springframework.stereotype.Component;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 12; // ✅ Increased from 8
    private static final int MAX_LENGTH = 128; // ✅ Increased from 100

    // ✅ Requires: uppercase, lowercase, number, special char
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
        "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&#^()_+=\\-\\[\\]{}|;:',.<>]).{" + 
        MIN_LENGTH + "," + MAX_LENGTH + "}$"
    );

    // ✅ Common weak passwords to reject
    private static final List<String> COMMON_PASSWORDS = Arrays.asList(
        "password", "12345678", "123456789", "qwerty", "abc123", "monkey",
        "letmein", "trustno1", "dragon", "baseball", "iloveyou", "master",
        "sunshine", "ashley", "bailey", "passw0rd", "shadow", "superman",
        "qazwsx", "michael", "football", "welcome", "jesus", "ninja",
        "mustang", "password1", "password123", "admin", "root", "user"
    );

    /**
     * Validates password against complexity requirements
     */
    public boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        
        // Check pattern
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            return false;
        }
        
        // ✅ Check against common passwords (case-insensitive)
        String lowerPassword = password.toLowerCase();
        if (COMMON_PASSWORDS.stream().anyMatch(lowerPassword::contains)) {
            return false;
        }
        
        // ✅ Check for sequential characters
        if (containsSequentialChars(password)) {
            return false;
        }
        
        return true;
    }

    /**
     * Validates password doesn't contain user's personal information
     */
    public boolean isValidWithUserInfo(String password, String email, String name) {
        if (!isValid(password)) {
            return false;
        }
        
        String lowerPassword = password.toLowerCase();
        
        // ✅ Check password doesn't contain email parts
        if (email != null) {
            String emailPrefix = email.split("@")[0].toLowerCase();
            if (lowerPassword.contains(emailPrefix)) {
                return false;
            }
        }
        
        // ✅ Check password doesn't contain name
        if (name != null) {
            String lowerName = name.toLowerCase().replaceAll("\\s+", "");
            if (lowerName.length() >= 3 && lowerPassword.contains(lowerName)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Check for sequential characters like "123", "abc", "qwerty"
     */
    private boolean containsSequentialChars(String password) {
        String lower = password.toLowerCase();
        
        // Check numeric sequences
        for (int i = 0; i < lower.length() - 2; i++) {
            char c1 = lower.charAt(i);
            char c2 = lower.charAt(i + 1);
            char c3 = lower.charAt(i + 2);
            
            // Numeric sequence (123, 234, etc.)
            if (Character.isDigit(c1) && Character.isDigit(c2) && Character.isDigit(c3)) {
                if (c2 == c1 + 1 && c3 == c2 + 1) {
                    return true;
                }
            }
            
            // Alphabetic sequence (abc, bcd, etc.)
            if (Character.isLetter(c1) && Character.isLetter(c2) && Character.isLetter(c3)) {
                if (c2 == c1 + 1 && c3 == c2 + 1) {
                    return true;
                }
            }
        }
        
        // Check keyboard sequences
        String[] keyboardSequences = {
            "qwerty", "asdfgh", "zxcvbn", "qwertz", "azerty"
        };
        
        for (String seq : keyboardSequences) {
            if (lower.contains(seq)) {
                return true;
            }
        }
        
        return false;
    }

    public String getRequirements() {
        return "Password must be at least " + MIN_LENGTH + " characters long and contain:\n" +
               "- At least one uppercase letter (A-Z)\n" +
               "- At least one lowercase letter (a-z)\n" +
               "- At least one number (0-9)\n" +
               "- At least one special character (@$!%*?&#^()_+=-[]{}|;:',.<>)\n" +
               "- No common passwords or sequential characters";
    }
    
    public String getShortRequirements() {
        return "At least " + MIN_LENGTH + " characters with uppercase, lowercase, number, and special character";
    }
}