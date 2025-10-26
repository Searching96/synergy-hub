package com.synergyhub.exception;

public class InvalidProjectMemberException extends BadRequestException {

    public InvalidProjectMemberException(String message) {
        super(message);
    }

    public static InvalidProjectMemberException alreadyMember(Integer userId, Integer projectId) {
        return new InvalidProjectMemberException(
                "User " + userId + " is already a member of project " + projectId
        );
    }

    public static InvalidProjectMemberException notAMember(Integer userId, Integer projectId) {
        return new InvalidProjectMemberException(
                "User " + userId + " is not a member of project " + projectId
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
