package com.synergyhub.exception;

public class PermissionNotFoundException extends RuntimeException {

    public PermissionNotFoundException(Integer permissionId) {
        super("Permission with ID " + permissionId + " not found");
    }

    public PermissionNotFoundException(String message) {
        super(message);
    }
}
