package com.synergyhub.common.exception;

public class InvalidTwoFactorCodeException extends UnauthorizedException {
    public InvalidTwoFactorCodeException(String message) {
        super(message);
    }
}