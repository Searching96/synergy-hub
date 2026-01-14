package com.synergyhub.exception;

public class TaskNotFoundException extends ResourceNotFoundException {

    public TaskNotFoundException(Long taskId) {
        super("Task", "id", taskId);
    }

    public TaskNotFoundException(String message) {
        super(message);
    }
}
