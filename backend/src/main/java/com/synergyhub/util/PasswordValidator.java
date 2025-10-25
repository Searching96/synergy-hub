package com.synergyhub.util;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 100;

    // At least one letter and one number
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(
            "^(?=.*[A-Za-z])(?=.*\\d).{" + MIN_LENGTH + "," + MAX_LENGTH + "}$"
    );

    public boolean isValid(String password) {
        if (password == null) {
            return false;
        }
        return PASSWORD_PATTERN.matcher(password).matches();
    }

    public String getRequirements() {
        return "Password must be at least " + MIN_LENGTH + " characters long and contain both letters and numbers";
    }
}