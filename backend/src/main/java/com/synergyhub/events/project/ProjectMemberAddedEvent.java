package com.synergyhub.events.project;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.ProjectRole;
import lombok.Getter;

@Getter
public class ProjectMemberAddedEvent extends ProjectEvent {
    private final User addedUser;
    private final ProjectRole role;

    public ProjectMemberAddedEvent(Project project, User addedUser, ProjectRole role, User actor, String ipAddress) {
        super(project, actor, ipAddress);
        this.addedUser = addedUser;
        this.role = role;
    }
}