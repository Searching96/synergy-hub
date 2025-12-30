package com.synergyhub.events.project;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;

public class ProjectUpdatedEvent extends ProjectEvent {
    public ProjectUpdatedEvent(Project project, User actor, String ipAddress) {
        super(project, actor, ipAddress);
    }
}