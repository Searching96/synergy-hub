package com.synergyhub.exception;

public class TwoFactorAuthenticationException extends RuntimeException {
    
    public TwoFactorAuthenticationException(String message) {
        super(message);
    }
}