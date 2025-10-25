package com.synergyhub.common.exception;

public class AccountLockedException extends UnauthorizedException {
    public AccountLockedException(String message) {
        super(message);
    }
}