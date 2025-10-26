package com.synergyhub.exception;

public class ProjectNameAlreadyExistsException extends BadRequestException {

    public ProjectNameAlreadyExistsException(String projectName, Integer organizationId) {
        super("Project with name '" + projectName + "' already exists in organization " + organizationId);
    }
}
