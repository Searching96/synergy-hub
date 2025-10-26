package com.synergyhub.exception;

public class InvalidSprintStateException extends BadRequestException {

    public InvalidSprintStateException(String message) {
        super(message);
    }

    public InvalidSprintStateException(String currentState, String requiredState) {
        super(String.format("Sprint is in '%s' state. Required state: '%s'", currentState, requiredState));
    }
}
