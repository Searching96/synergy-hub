package com.synergyhub.repository;

import com.synergyhub.domain.entity.Task;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.domain.enums.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    // CRITICAL: These methods leak data across organizations! They should only be used internally
    // with proper organization context checking
    List<Task> findByProjectId(Long projectId);
    List<Task> findBySprintIdOrderByPriorityDescCreatedAtAsc(Long sprintId);
    List<Task> findByProjectIdAndSprintIsNullOrderByPriorityDescCreatedAtAsc(Long projectId);
    List<Task> findByAssigneeIdOrderByPriorityDescCreatedAtAsc(Long assigneeId);
    List<Task> findByProjectIdAndStatus(Long projectId, TaskStatus status);
    List<Task> findByParentTaskId(Long parentTaskId);
    List<Task> findByEpicId(Long epicId);
    List<Task> findByProjectIdAndType(Long projectId, TaskType type);

    // Organization-scoped versions (safe)
    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.project.organization.id = :orgId")
    List<Task> findByProjectIdInOrganization(@Param("projectId") Long projectId, @Param("orgId") Long orgId);

    @Query("SELECT t FROM Task t WHERE t.sprint.id = :sprintId AND t.project.organization.id = :orgId ORDER BY t.priority DESC, t.createdAt ASC")
    List<Task> findBySprintIdOrderByPriorityDescCreatedAtAscInOrganization(@Param("sprintId") Long sprintId, @Param("orgId") Long orgId);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.sprint IS NULL AND t.project.organization.id = :orgId ORDER BY t.priority DESC, t.createdAt ASC")
    List<Task> findByProjectIdAndSprintIsNullOrderByPriorityDescCreatedAtAscInOrganization(@Param("projectId") Long projectId, @Param("orgId") Long orgId);

    @Query("SELECT t FROM Task t WHERE t.assignee.id = :assigneeId AND t.project.organization.id = :orgId ORDER BY t.priority DESC, t.createdAt ASC")
    List<Task> findByAssigneeIdOrderByPriorityDescCreatedAtAscInOrganization(@Param("assigneeId") Long assigneeId, @Param("orgId") Long orgId);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.status = :status AND t.project.organization.id = :orgId")
    List<Task> findByProjectIdAndStatusInOrganization(@Param("projectId") Long projectId, @Param("status") TaskStatus status, @Param("orgId") Long orgId);

    @Query("SELECT t FROM Task t WHERE t.parentTask.id = :parentTaskId AND t.project.organization.id = :orgId")
    List<Task> findByParentTaskIdInOrganization(@Param("parentTaskId") Long parentTaskId, @Param("orgId") Long orgId);

    @Query("SELECT t FROM Task t WHERE t.epic.id = :epicId AND t.project.organization.id = :orgId")
    List<Task> findByEpicIdInOrganization(@Param("epicId") Long epicId, @Param("orgId") Long orgId);

    @Query("SELECT t FROM Task t WHERE t.project.id = :projectId AND t.type = :type AND t.project.organization.id = :orgId")
    List<Task> findByProjectIdAndTypeInOrganization(@Param("projectId") Long projectId, @Param("type") TaskType type, @Param("orgId") Long orgId);

    // CRITICAL: This method was leaking across all organizations!
    @Query("SELECT t FROM Task t WHERE t.dueDate < CURRENT_TIMESTAMP AND t.status NOT IN ('DONE', 'CANCELLED')")
    List<Task> findOverdueTasks();

    @Query("SELECT t FROM Task t WHERE t.dueDate < CURRENT_TIMESTAMP AND t.status NOT IN ('DONE', 'CANCELLED') AND t.project.organization.id = :orgId")
    List<Task> findOverdueTasksInOrganization(@Param("orgId") Long orgId);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.sprint LEFT JOIN FETCH t.assignee WHERE t.project.id = :projectId")
    List<Task> findByProjectIdWithDetails(@Param("projectId") Long projectId);

    @Query("SELECT t FROM Task t LEFT JOIN FETCH t.sprint LEFT JOIN FETCH t.assignee WHERE t.project.id = :projectId AND t.project.organization.id = :orgId")
    List<Task> findByProjectIdWithDetailsInOrganization(@Param("projectId") Long projectId, @Param("orgId") Long orgId);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END " +
            "FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    boolean hasAccessToTaskProject(@Param("projectId") Long projectId, @Param("userId") Long userId);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END " +
            "FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId AND pm.project.organization.id = :orgId")
    boolean hasAccessToTaskProjectInOrganization(@Param("projectId") Long projectId, @Param("userId") Long userId, @Param("orgId") Long orgId);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.sprint.id = :sprintId AND t.status = :status")
    long countBySprintIdAndStatus(@Param("sprintId") Long sprintId, @Param("status") TaskStatus status);

    @Query("SELECT COUNT(t) FROM Task t WHERE t.sprint.id = :sprintId AND t.status = :status AND t.project.organization.id = :orgId")
    long countBySprintIdAndStatusInOrganization(@Param("sprintId") Long sprintId, @Param("status") TaskStatus status, @Param("orgId") Long orgId);

    // Organization-scoped query: derived method
    List<Task> findAllByProjectOrganizationOrgId(Long orgId);

    // Organization-scoped query: explicit JPQL
    @Query("SELECT t FROM Task t WHERE t.project.organization.orgId = :orgId")
    List<Task> findAllByOrganizationId(@Param("orgId") Long orgId);
}
