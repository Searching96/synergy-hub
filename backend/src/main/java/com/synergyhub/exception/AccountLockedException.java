package com.synergyhub.exception;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AccountLockedException extends RuntimeException {
    
    private final LocalDateTime lockUntil;
    
    public AccountLockedException(String message, LocalDateTime lockUntil) {
        super(message);
        this.lockUntil = lockUntil;
    }

}