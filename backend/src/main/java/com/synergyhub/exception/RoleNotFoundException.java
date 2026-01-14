package com.synergyhub.exception;

public class RoleNotFoundException extends RuntimeException {

    public RoleNotFoundException(Long roleId) {
        super("Role with ID " + roleId + " not found");
    }

    public RoleNotFoundException(String message) {
        super(message);
    }
}
