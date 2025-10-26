package com.synergyhub.dto.mapper;

import com.synergyhub.domain.entity.Task;
import com.synergyhub.dto.response.TaskResponse;
import com.synergyhub.dto.response.TaskSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring", uses = {UserMapper.class})
public interface TaskMapper {

    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    @Mapping(target = "sprintId", source = "sprint.id")
    @Mapping(target = "sprintName", source = "sprint.name")
    @Mapping(target = "assignee", source = "assignee")
    @Mapping(target = "creator", source = "creator")
    @Mapping(target = "isOverdue", expression = "java(task.isOverdue())")
    TaskResponse toTaskResponse(Task task);

    @Mapping(target = "assigneeId", source = "assignee.id")
    @Mapping(target = "assigneeName", source = "assignee.name")
    @Mapping(target = "isOverdue", expression = "java(task.isOverdue())")
    TaskSummaryResponse toTaskSummaryResponse(Task task);

    List<TaskResponse> toTaskResponseList(List<Task> tasks);

    List<TaskSummaryResponse> toTaskSummaryResponseList(List<Task> tasks);
}
