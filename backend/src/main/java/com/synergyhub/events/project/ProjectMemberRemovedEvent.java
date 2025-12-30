package com.synergyhub.events.project;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;
import lombok.Getter;

@Getter
public class ProjectMemberRemovedEvent extends ProjectEvent {
    private final Integer removedUserId;

    public ProjectMemberRemovedEvent(Project project, Integer removedUserId, User actor, String ipAddress) {
        super(project, actor, ipAddress);
        this.removedUserId = removedUserId;
    }
}