package com.synergyhub.exception;

public class UnauthorizedProjectAccessException extends RuntimeException {

    public UnauthorizedProjectAccessException(Integer projectId, Integer userId) {
        super("User " + userId + " is not authorized to access project " + projectId);
    }

    public UnauthorizedProjectAccessException(String message) {
        super(message);
    }
}
