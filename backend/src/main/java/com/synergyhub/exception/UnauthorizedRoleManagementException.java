package com.synergyhub.exception;

public class UnauthorizedRoleManagementException extends RuntimeException {

    public UnauthorizedRoleManagementException(Integer organizationId) {
        super("User is not an admin of organization " + organizationId + " and cannot manage roles");
    }

    public UnauthorizedRoleManagementException(String message) {
        super(message);
    }
}
