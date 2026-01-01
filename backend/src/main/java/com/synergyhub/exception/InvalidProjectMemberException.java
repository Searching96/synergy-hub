package com.synergyhub.exception;

public class InvalidProjectMemberException extends BadRequestException {

    public InvalidProjectMemberException(String message) {
        super(message);
    }

    public static InvalidProjectMemberException alreadyMember(String email, String projectName) {
        return new InvalidProjectMemberException(
                "User '" + email + "' is already a member of project '" + projectName + "'"
        );
    }

    public static InvalidProjectMemberException notAMember(String email, String projectName) {
        return new InvalidProjectMemberException(
                "User '" + email + "' is not a member of project '" + projectName + "'"
        );
    }

    public static InvalidProjectMemberException cannotRemoveProjectLead() {
        return new InvalidProjectMemberException(
                "Cannot remove project lead from project members"
        );
    }

    public static InvalidProjectMemberException differentOrganization() {
        return new InvalidProjectMemberException(
                "Cannot add member from different organization"
        );
    }
}
