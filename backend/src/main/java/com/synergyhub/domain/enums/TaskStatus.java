package com.synergyhub.domain.enums;

import lombok.Getter;

@Getter
public enum TaskStatus {
    TO_DO("To Do"),
    IN_PROGRESS("In Progress"),
    IN_REVIEW("In Review"),
    DONE("Done"),
    BLOCKED("Blocked"),
    CANCELLED("Cancelled"),
    BACKLOG("Backlog");
    
    private final String displayName;
    
    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

}