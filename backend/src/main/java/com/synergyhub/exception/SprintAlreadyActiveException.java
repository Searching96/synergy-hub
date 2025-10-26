package com.synergyhub.exception;

public class SprintAlreadyActiveException extends BadRequestException {

    public SprintAlreadyActiveException(String projectName) {
        super(String.format("Project '%s' already has an active sprint", projectName));
    }

    public SprintAlreadyActiveException(Integer projectId, Integer activeSprintId) {
        super(String.format("Project %d already has active sprint with ID: %d", projectId, activeSprintId));
    }
}
