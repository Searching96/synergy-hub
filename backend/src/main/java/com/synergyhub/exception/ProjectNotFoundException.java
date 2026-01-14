package com.synergyhub.exception;

public class ProjectNotFoundException extends ResourceNotFoundException {

    public ProjectNotFoundException(Long projectId) {
        super("Project", "id", projectId);
    }

    public ProjectNotFoundException(String projectName, Long organizationId) {
        super("Project not found with name '" + projectName + "' in organization " + organizationId);
    }
}
