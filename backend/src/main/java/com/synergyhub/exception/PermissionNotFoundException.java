package com.synergyhub.exception;

public class PermissionNotFoundException extends RuntimeException {

    public PermissionNotFoundException(Long permissionId) {
        super("Permission with ID " + permissionId + " not found");
    }

    public PermissionNotFoundException(String message) {
        super(message);
    }
}
