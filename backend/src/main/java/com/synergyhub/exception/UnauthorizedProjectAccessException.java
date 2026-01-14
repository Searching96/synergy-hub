package com.synergyhub.exception;

public class UnauthorizedProjectAccessException extends RuntimeException {

    public UnauthorizedProjectAccessException(Long projectId, Long userId) {
        super("User " + userId + " is not authorized to access project " + projectId);
    }

    public UnauthorizedProjectAccessException(String message) {
        super(message);
    }
}
