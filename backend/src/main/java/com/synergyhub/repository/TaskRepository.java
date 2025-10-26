package com.synergyhub.repository;

import com.synergyhub.domain.entity.Task;
import com.synergyhub.domain.enums.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Integer> {

    // Find tasks by project
    List<Task> findByProjectId(Integer projectId);

    // Find tasks by sprint
    List<Task> findBySprintIdOrderByPriorityDescCreatedAtAsc(Integer sprintId);

    // Find tasks in backlog (no sprint assigned)
    List<Task> findByProjectIdAndSprintIsNullOrderByPriorityDescCreatedAtAsc(Integer projectId);

    // Find tasks by assignee
    List<Task> findByAssigneeIdOrderByPriorityDescCreatedAtAsc(Integer assigneeId);

    // Find tasks by status
    List<Task> findByProjectIdAndStatus(Integer projectId, TaskStatus status);

    // Find overdue tasks
    @Query("SELECT t FROM Task t WHERE t.dueDate < CURRENT_TIMESTAMP AND t.status NOT IN ('DONE', 'CANCELLED')")
    List<Task> findOverdueTasks();

    // Find tasks by project with sprint info
    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.sprint LEFT JOIN FETCH t.assignee WHERE t.project.id = :projectId")
    List<Task> findByProjectIdWithDetails(@Param("projectId") Integer projectId);

    // Check if user has access to task through project membership
    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END " +
            "FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    boolean hasAccessToTaskProject(@Param("projectId") Integer projectId, @Param("userId") Integer userId);

    // Count tasks by status in sprint
    @Query("SELECT COUNT(t) FROM Task t WHERE t.sprint.id = :sprintId AND t.status = :status")
    long countBySprintIdAndStatus(@Param("sprintId") Integer sprintId, @Param("status") TaskStatus status);
}
