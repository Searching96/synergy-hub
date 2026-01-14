package com.synergyhub.events.project;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;
import lombok.Getter;

@Getter
public class ProjectMemberRemovedEvent extends ProjectEvent {
    private final Long removedUserId;
    
    public ProjectMemberRemovedEvent(Project project, Long removedUserId,
                                     User actor, String ipAddress) {
        super(project, actor, ipAddress);
        this.removedUserId = removedUserId;
    }
}