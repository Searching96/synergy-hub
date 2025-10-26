package com.synergyhub.exception;

public class ProjectNotFoundException extends ResourceNotFoundException {

    public ProjectNotFoundException(Integer projectId) {
        super("Project", "id", projectId);
    }

    public ProjectNotFoundException(String projectName, Integer organizationId) {
        super("Project not found with name '" + projectName + "' in organization " + organizationId);
    }
}
