package com.synergyhub.exception;

public class TaskAssignmentException extends BadRequestException {

    public TaskAssignmentException(String message) {
        super(message);
    }

    public TaskAssignmentException(Integer taskId, Integer userId) {
        super(String.format("Cannot assign task %d to user %d", taskId, userId));
    }
}
