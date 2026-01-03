package com.synergyhub.exception;

public class OrganizationNotFoundException extends RuntimeException {

    public OrganizationNotFoundException(Integer organizationId) {
        super("Organization with ID " + organizationId + " not found");
    }

    public OrganizationNotFoundException(String message) {
        super(message);
    }
}
