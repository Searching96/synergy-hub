package com.synergyhub.exception;

public class TaskAssignmentException extends BadRequestException {

    public TaskAssignmentException(String message) {
        super(message);
    }

    public TaskAssignmentException(Long taskId, Long userId) {
        super(String.format("Cannot assign task %d to user %d", taskId, userId));
    }
}
