package com.synergyhub.exception;

public class RoleNameAlreadyExistsException extends RuntimeException {

    public RoleNameAlreadyExistsException(String roleName) {
        super("Role with name '" + roleName + "' already exists");
    }
}
