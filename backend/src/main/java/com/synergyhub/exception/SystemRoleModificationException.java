package com.synergyhub.exception;

public class SystemRoleModificationException extends RuntimeException {

    public SystemRoleModificationException(String roleName) {
        super("Cannot modify system role: " + roleName + ". System roles are protected.");
    }

    public SystemRoleModificationException(String message, Throwable cause) {
        super(message, cause);
    }
}
