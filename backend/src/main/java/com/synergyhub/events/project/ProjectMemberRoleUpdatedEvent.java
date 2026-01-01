package com.synergyhub.events.project;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;
import com.synergyhub.domain.enums.ProjectRole;
import lombok.Getter;

@Getter
public class ProjectMemberRoleUpdatedEvent extends ProjectEvent {
    private final Integer userId;
    private final ProjectRole newRole;
    
    public ProjectMemberRoleUpdatedEvent(Project project, Integer userId, 
                                         ProjectRole newRole, User actor, String ipAddress) {
        super(project, actor, ipAddress);
        this.userId = userId;
        this.newRole = newRole;
    }
}