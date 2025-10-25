package com.synergyhub.common.exception;

public class InvalidTokenException extends InvalidRequestException {
    public InvalidTokenException(String message) {
        super(message);
    }
}