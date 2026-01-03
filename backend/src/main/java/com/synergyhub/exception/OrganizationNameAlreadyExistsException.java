package com.synergyhub.exception;

public class OrganizationNameAlreadyExistsException extends RuntimeException {

    public OrganizationNameAlreadyExistsException(String organizationName) {
        super("Organization with name '" + organizationName + "' already exists");
    }
}
