package com.synergyhub.exception;

public class UserAlreadyInOrganizationException extends RuntimeException {
    public UserAlreadyInOrganizationException(String message) {
        super(message);
    }
}
