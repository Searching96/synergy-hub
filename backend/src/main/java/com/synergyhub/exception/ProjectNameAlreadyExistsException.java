package com.synergyhub.exception;

public class ProjectNameAlreadyExistsException extends BadRequestException {

    public ProjectNameAlreadyExistsException(String projectName, Long organizationId) {
        super("Project with name '" + projectName + "' already exists in organization " + organizationId);
    }
}
