package com.synergyhub.exception;

public class SprintNotFoundException extends ResourceNotFoundException {

    public SprintNotFoundException(Long sprintId) {
        super("Sprint", "id", sprintId);
    }

    public SprintNotFoundException(String message) {
        super(message);
    }
}
