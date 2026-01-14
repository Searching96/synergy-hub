package com.synergyhub.exception;

public class UnauthorizedOrganizationAccessException extends RuntimeException {

    public UnauthorizedOrganizationAccessException(Long organizationId, Long userId) {
        super("User " + userId + " is not authorized to access organization " + organizationId);
    }

    public UnauthorizedOrganizationAccessException(String message) {
        super(message);
    }
}

