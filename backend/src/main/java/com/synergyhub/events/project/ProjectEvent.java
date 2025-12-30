package com.synergyhub.events.project;

import com.synergyhub.domain.entity.Project;
import com.synergyhub.domain.entity.User;
import com.synergyhub.events.BaseEvent;

import lombok.Getter;

@Getter
public abstract class ProjectEvent extends BaseEvent {
    private final Project project;

    protected ProjectEvent(Project project, User actor, String ipAddress) {
        super(actor, ipAddress);
        this.project = project;
    }
}