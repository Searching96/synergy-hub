package com.synergyhub.dto.mapper;

import com.synergyhub.domain.entity.Task;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.dto.response.TaskResponse;
import com.synergyhub.dto.response.TaskSummaryResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TaskMapper {

    // ========== FULL TASK RESPONSE ==========
    
    @Mapping(target = "projectId", source = "project.id")
    @Mapping(target = "projectName", source = "project.name")
    
    @Mapping(target = "sprintId", source = "sprint.id")
    @Mapping(target = "sprintName", source = "sprint.name")
    
    @Mapping(target = "parentTaskId", source = "parentTask.id")
    @Mapping(target = "parentTaskTitle", source = "parentTask.title")
    @Mapping(target = "isSubtask", expression = "java(task.getParentTask() != null)")
    
    @Mapping(target = "epicId", source = "epic.id")
    @Mapping(target = "epicTitle", source = "epic.title")
    
    // ✅ People (using nested mapping)
    @Mapping(target = "reporter", source = "reporter", qualifiedByName = "userToSummary")
    @Mapping(target = "assignee", source = "assignee", qualifiedByName = "userToSummary")
    
    // ✅ Subtasks (using summary to avoid deep nesting)
    @Mapping(target = "subtasks", source = "subtasks")
    
    // ✅ Computed fields
    @Mapping(target = "completionPercentage", expression = "java(task.getCompletionPercentage())")
    @Mapping(target = "isOverdue", expression = "java(task.isOverdue())")
    @Mapping(target = "subtaskCount", expression = "java(task.getSubtasks().size())")
    @Mapping(target = "completedSubtaskCount", source = "subtasks", qualifiedByName = "countCompletedSubtasks")
    
    @Mapping(target = "watchersCount", expression = "java(task.getWatchers().size())")
    @Mapping(target = "linkedTasks", source = "linkedTasks")
    
    TaskResponse toTaskResponse(Task task);

    // ========== TASK SUMMARY RESPONSE ==========
    
    @Mapping(target = "assigneeId", source = "assignee.id")
    @Mapping(target = "assigneeName", source = "assignee.name")
    
    @Mapping(target = "reporterId", source = "reporter.id")
    @Mapping(target = "reporterName", source = "reporter.name")
    
    @Mapping(target = "isOverdue", expression = "java(task.isOverdue())")
    @Mapping(target = "isSubtask", expression = "java(task.getParentTask() != null)")
    
    @Mapping(target = "subtaskCount", expression = "java(task.getSubtasks().size())")
    @Mapping(target = "completionPercentage", expression = "java(task.getCompletionPercentage())")
    
    TaskSummaryResponse toTaskSummaryResponse(Task task);

    // ========== LIST MAPPINGS ==========
    
    List<TaskResponse> toTaskResponseList(List<Task> tasks);
    List<TaskSummaryResponse> toTaskSummaryResponseList(List<Task> tasks);

    // ========== HELPER METHODS ==========
    
    /**
     * Map User entity to lightweight UserSummary.
     * Handles null users gracefully.
     */
    @Named("userToSummary")
    default TaskResponse.UserSummary userToSummary(com.synergyhub.domain.entity.User user) {
        if (user == null) {
            return null;
        }
        
        return TaskResponse.UserSummary.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .build();
    }

    /**
     * Count completed subtasks for the completedSubtaskCount field.
     */
    @Named("countCompletedSubtasks")
    default int countCompletedSubtasks(List<Task> subtasks) {
        if (subtasks == null || subtasks.isEmpty()) {
            return 0;
        }
        
        return (int) subtasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();
    }
}