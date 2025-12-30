package com.synergyhub.events.project;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;

public class ProjectArchivedEvent extends ProjectEvent {
    public ProjectArchivedEvent(Project project, User actor, String ipAddress) {
        super(project, actor, ipAddress);
    }
}