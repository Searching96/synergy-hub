package com.synergyhub.exception;

import java.time.LocalDateTime;

public class AccountLockedException extends RuntimeException {
    
    private final LocalDateTime lockUntil;
    
    public AccountLockedException(String message, LocalDateTime lockUntil) {
        super(message);
        this.lockUntil = lockUntil;
    }
    
    public LocalDateTime getLockUntil() {
        return lockUntil;
    }
}