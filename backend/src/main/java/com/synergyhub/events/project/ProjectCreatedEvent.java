package com.synergyhub.events.project;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;

public class ProjectCreatedEvent extends ProjectEvent {
    public ProjectCreatedEvent(Project project, User actor, String ipAddress) {
        super(project, actor, ipAddress);
    }
}
