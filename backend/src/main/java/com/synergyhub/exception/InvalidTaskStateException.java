package com.synergyhub.exception;

public class InvalidTaskStateException extends BadRequestException {

    public InvalidTaskStateException(String message) {
        super(message);
    }

    public InvalidTaskStateException(String currentState, String requiredState) {
        super(String.format("Task is in '%s' state. Required state: '%s'", currentState, requiredState));
    }
}
