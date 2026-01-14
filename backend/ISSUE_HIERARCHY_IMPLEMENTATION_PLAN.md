# Backend Implementation Plan: Jira-Style Issue Hierarchy

**Date:** January 6, 2026  
**Status:** Not Started  
**Priority:** High

## Overview

This document outlines the backend implementation required to support Jira-style issue hierarchy with Epics owning Stories/Tasks/Bugs, which in turn own Subtasks. The frontend implementation is already complete and uses mock data.

## 1. Database Schema Changes

### 1.1 Update Task/Issue Table

Add the following columns to the `task` table:

```sql
ALTER TABLE task 
ADD COLUMN parent_task_id BIGINT NULL,
ADD COLUMN epic_id BIGINT NULL,
ADD CONSTRAINT fk_task_parent FOREIGN KEY (parent_task_id) REFERENCES task(id) ON DELETE CASCADE,
ADD CONSTRAINT fk_task_epic FOREIGN KEY (epic_id) REFERENCES task(id) ON DELETE SET NULL;

-- Add indexes for performance
CREATE INDEX idx_task_parent_task_id ON task(parent_task_id);
CREATE INDEX idx_task_epic_id ON task(epic_id);
CREATE INDEX idx_task_type ON task(type);
```

### 1.2 Add SUBTASK Type to Enum

Update the task type enum if it's defined in the database:

```sql
-- If using PostgreSQL enum
ALTER TYPE task_type ADD VALUE 'SUBTASK';

-- Or update constraint if using CHECK constraint
ALTER TABLE task DROP CONSTRAINT IF EXISTS task_type_check;
ALTER TABLE task ADD CONSTRAINT task_type_check 
  CHECK (type IN ('EPIC', 'STORY', 'TASK', 'BUG', 'SUBTASK'));
```

## 2. Backend Entity/Model Updates

### 2.1 Update Task Entity (Java)

```java
@Entity
@Table(name = "task")
public class Task {
    // ... existing fields ...
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "epic_id")
    private Task epic;
    
    @OneToMany(mappedBy = "parentTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Task> subtasks = new ArrayList<>();
    
    @OneToMany(mappedBy = "epic")
    private List<Task> childIssues = new ArrayList<>();
    
    // Add validation in setters or @PrePersist
    @PrePersist
    @PreUpdate
    private void validateHierarchy() {
        // SUBTASK must have a parent
        if (this.type == TaskType.SUBTASK && this.parentTask == null) {
            throw new IllegalStateException("Subtask must have a parent task");
        }
        
        // SUBTASK cannot have an epic (gets it from parent)
        if (this.type == TaskType.SUBTASK && this.epic != null) {
            throw new IllegalStateException("Subtask cannot directly belong to an epic");
        }
        
        // EPIC cannot have a parent or belong to another epic
        if (this.type == TaskType.EPIC && (this.parentTask != null || this.epic != null)) {
            throw new IllegalStateException("Epic cannot have a parent or belong to another epic");
        }
        
        // Only STORY, TASK, BUG can have subtasks
        if (this.parentTask != null) {
            TaskType parentType = this.parentTask.getType();
            if (parentType != TaskType.STORY && parentType != TaskType.TASK && parentType != TaskType.BUG) {
                throw new IllegalStateException("Parent must be a Story, Task, or Bug");
            }
        }
    }
}
```

### 2.2 Update TaskType Enum

```java
public enum TaskType {
    EPIC,
    STORY,
    TASK,
    BUG,
    SUBTASK
}
```

## 3. DTO Updates

### 3.1 Update TaskResponse DTO

```java
@Data
public class TaskResponse {
    // ... existing fields ...
    
    private Long parentTaskId;
    private Long epicId;
    
    // Nested objects for richer response
    private TaskSummary parentTask;
    private TaskSummary epic;
    private List<TaskSummary> subtasks;
    
    @Data
    public static class TaskSummary {
        private Long id;
        private String title;
        private TaskType type;
        private TaskStatus status;
    }
}
```

### 3.2 Update CreateTaskRequest DTO

```java
@Data
public class CreateTaskRequest {
    // ... existing fields ...
    
    private Long parentTaskId;  // Required for SUBTASK
    private Long epicId;        // Optional for STORY, TASK, BUG
    
    @AssertTrue(message = "Subtask must have a parent task")
    public boolean isValidSubtask() {
        if (type == TaskType.SUBTASK) {
            return parentTaskId != null;
        }
        return true;
    }
    
    @AssertTrue(message = "Epic cannot have parent or epic link")
    public boolean isValidEpic() {
        if (type == TaskType.EPIC) {
            return parentTaskId == null && epicId == null;
        }
        return true;
    }
}
```

### 3.3 Update UpdateTaskRequest DTO

```java
@Data
public class UpdateTaskRequest {
    // ... existing fields ...
    
    private Long parentTaskId;
    private Long epicId;
    
    // Similar validation as CreateTaskRequest
}
```

## 4. Service Layer Updates

### 4.1 Update TaskService

```java
@Service
public class TaskService {
    
    @Transactional
    public TaskResponse createTask(CreateTaskRequest request, Long userId) {
        // Validate hierarchy relationships
        validateHierarchyRelationships(request);
        
        Task task = new Task();
        // ... map basic fields ...
        
        // Set parent task if subtask
        if (request.getParentTaskId() != null) {
            Task parent = taskRepository.findById(request.getParentTaskId())
                .orElseThrow(() -> new ResourceNotFoundException("Parent task not found"));
            
            // Validate parent is in same project
            if (!parent.getProjectId().equals(request.getProjectId())) {
                throw new IllegalArgumentException("Parent task must be in the same project");
            }
            
            // Validate parent type
            if (!canBeParent(parent.getType())) {
                throw new IllegalArgumentException("Parent must be a Story, Task, or Bug");
            }
            
            task.setParentTask(parent);
        }
        
        // Set epic if applicable
        if (request.getEpicId() != null) {
            Task epic = taskRepository.findById(request.getEpicId())
                .orElseThrow(() -> new ResourceNotFoundException("Epic not found"));
            
            // Validate epic type
            if (epic.getType() != TaskType.EPIC) {
                throw new IllegalArgumentException("Epic ID must reference an Epic type issue");
            }
            
            // Validate epic is in same project
            if (!epic.getProjectId().equals(request.getProjectId())) {
                throw new IllegalArgumentException("Epic must be in the same project");
            }
            
            task.setEpic(epic);
        }
        
        task = taskRepository.save(task);
        return mapToResponseWithHierarchy(task);
    }
    
    @Transactional
    public TaskResponse updateTask(Long taskId, UpdateTaskRequest request, Long userId) {
        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> new ResourceNotFoundException("Task not found"));
        
        // Validate hierarchy changes
        if (request.getParentTaskId() != null) {
            // Prevent circular references
            if (isCircularReference(taskId, request.getParentTaskId())) {
                throw new IllegalArgumentException("Circular parent reference detected");
            }
        }
        
        // ... update logic ...
        
        return mapToResponseWithHierarchy(task);
    }
    
    private boolean canBeParent(TaskType type) {
        return type == TaskType.STORY || type == TaskType.TASK || type == TaskType.BUG;
    }
    
    private boolean isCircularReference(Long taskId, Long parentId) {
        Task parent = taskRepository.findById(parentId).orElse(null);
        while (parent != null) {
            if (parent.getId().equals(taskId)) {
                return true;
            }
            parent = parent.getParentTask();
        }
        return false;
    }
    
    private TaskResponse mapToResponseWithHierarchy(Task task) {
        TaskResponse response = new TaskResponse();
        // ... map basic fields ...
        
        // Map parent task
        if (task.getParentTask() != null) {
            response.setParentTaskId(task.getParentTask().getId());
            response.setParentTask(mapToSummary(task.getParentTask()));
        }
        
        // Map epic
        if (task.getEpic() != null) {
            response.setEpicId(task.getEpic().getId());
            response.setEpic(mapToSummary(task.getEpic()));
        }
        
        // Map subtasks (don't fetch recursively to avoid performance issues)
        if (task.getType() != TaskType.SUBTASK) {
            List<Task> subtasks = taskRepository.findByParentTaskId(task.getId());
            response.setSubtasks(subtasks.stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList()));
        }
        
        // Map child issues for epics
        if (task.getType() == TaskType.EPIC) {
            List<Task> childIssues = taskRepository.findByEpicId(task.getId());
            response.setSubtasks(childIssues.stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList()));
        }
        
        return response;
    }
    
    private TaskResponse.TaskSummary mapToSummary(Task task) {
        TaskResponse.TaskSummary summary = new TaskResponse.TaskSummary();
        summary.setId(task.getId());
        summary.setTitle(task.getTitle());
        summary.setType(task.getType());
        summary.setStatus(task.getStatus());
        return summary;
    }
}
```

## 5. Repository Layer Updates

### 5.1 Add Queries to TaskRepository

```java
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    // ... existing methods ...
    
    // Find all subtasks of a parent task
    List<Task> findByParentTaskId(Long parentTaskId);
    
    // Find all issues belonging to an epic
    List<Task> findByEpicId(Long epicId);
    
    // Find all epics in a project
    List<Task> findByProjectIdAndType(Long projectId, TaskType type);
    
    // Find all potential parent tasks (Story, Task, Bug) in a project
    @Query("SELECT t FROM Task t WHERE t.projectId = :projectId AND t.type IN ('STORY', 'TASK', 'BUG') AND t.archived = false")
    List<Task> findPotentialParentsByProjectId(@Param("projectId") Long projectId);
    
    // Count subtasks for a task
    @Query("SELECT COUNT(t) FROM Task t WHERE t.parentTask.id = :taskId")
    Long countSubtasksByParentId(@Param("taskId") Long taskId);
    
    // Check if task has subtasks (to prevent deletion/archiving)
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Task t WHERE t.parentTask.id = :taskId")
    boolean hasSubtasks(@Param("taskId") Long taskId);
}
```

## 6. API Endpoint Updates

### 6.1 New Endpoints

Add these endpoints to `TaskController`:

```java
@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    
    /**
     * GET /api/tasks/{id}/subtasks
     * Get all subtasks of a task
     */
    @GetMapping("/{id}/subtasks")
    public ResponseEntity<List<TaskResponse>> getSubtasks(@PathVariable Long id) {
        List<TaskResponse> subtasks = taskService.getSubtasks(id);
        return ResponseEntity.ok(subtasks);
    }
    
    /**
     * GET /api/tasks/{id}/hierarchy
     * Get full hierarchy (parent, epic, subtasks) for a task
     */
    @GetMapping("/{id}/hierarchy")
    public ResponseEntity<TaskHierarchyResponse> getTaskHierarchy(@PathVariable Long id) {
        TaskHierarchyResponse hierarchy = taskService.getTaskHierarchy(id);
        return ResponseEntity.ok(hierarchy);
    }
    
    /**
     * GET /api/projects/{projectId}/epics
     * Get all epics in a project
     */
    @GetMapping("/projects/{projectId}/epics")
    public ResponseEntity<List<TaskResponse>> getProjectEpics(@PathVariable Long projectId) {
        List<TaskResponse> epics = taskService.getProjectEpics(projectId);
        return ResponseEntity.ok(epics);
    }
    
    /**
     * GET /api/projects/{projectId}/potential-parents
     * Get all tasks that can be parents (Story, Task, Bug) in a project
     */
    @GetMapping("/projects/{projectId}/potential-parents")
    public ResponseEntity<List<TaskResponse>> getPotentialParents(@PathVariable Long projectId) {
        List<TaskResponse> parents = taskService.getPotentialParents(projectId);
        return ResponseEntity.ok(parents);
    }
    
    /**
     * POST /api/tasks/{id}/move-to-epic
     * Move a task to a different epic
     */
    @PostMapping("/{id}/move-to-epic")
    public ResponseEntity<TaskResponse> moveToEpic(
        @PathVariable Long id,
        @RequestParam(required = false) Long epicId
    ) {
        TaskResponse task = taskService.moveToEpic(id, epicId);
        return ResponseEntity.ok(task);
    }
}
```

## 7. Business Rules & Validations

### 7.1 Hierarchy Rules

Implement these validation rules:

1. **EPIC rules:**
   - Cannot have a parent task
   - Cannot belong to another epic
   - Can have child issues (STORY, TASK, BUG)
   - Cannot be deleted if it has child issues

2. **STORY/TASK/BUG rules:**
   - Can optionally belong to an epic
   - Can have subtasks
   - Cannot be deleted/archived if they have incomplete subtasks
   - Cannot be moved to DONE if subtasks are not complete

3. **SUBTASK rules:**
   - Must have a parent (STORY, TASK, or BUG)
   - Cannot have its own subtasks
   - Cannot directly belong to an epic (inherits from parent)
   - Must be in the same project as parent

### 7.2 Validation Service

```java
@Service
public class TaskHierarchyValidator {
    
    public void validateTaskDeletion(Task task) {
        if (task.getType() == TaskType.EPIC) {
            long childCount = taskRepository.countByEpicId(task.getId());
            if (childCount > 0) {
                throw new IllegalStateException(
                    "Cannot delete epic with child issues. Remove or reassign child issues first."
                );
            }
        }
        
        if (canBeParent(task.getType())) {
            boolean hasSubtasks = taskRepository.hasSubtasks(task.getId());
            if (hasSubtasks) {
                throw new IllegalStateException(
                    "Cannot delete task with subtasks. Delete subtasks first."
                );
            }
        }
    }
    
    public void validateStatusChange(Task task, TaskStatus newStatus) {
        if (newStatus == TaskStatus.DONE && canBeParent(task.getType())) {
            List<Task> subtasks = taskRepository.findByParentTaskId(task.getId());
            boolean hasIncompleteSubtasks = subtasks.stream()
                .anyMatch(st -> st.getStatus() != TaskStatus.DONE);
            
            if (hasIncompleteSubtasks) {
                throw new IllegalStateException(
                    "Cannot mark task as done while it has incomplete subtasks"
                );
            }
        }
    }
    
    private boolean canBeParent(TaskType type) {
        return type == TaskType.STORY || type == TaskType.TASK || type == TaskType.BUG;
    }
}
```

## 8. Migration Script

### 8.1 Data Migration

```sql
-- Migration script to update existing data
-- Run this after schema changes are applied

-- Step 1: Backup existing data
CREATE TABLE task_backup AS SELECT * FROM task;

-- Step 2: Initialize new columns
UPDATE task SET parent_task_id = NULL WHERE parent_task_id IS NULL;
UPDATE task SET epic_id = NULL WHERE epic_id IS NULL;

-- Step 3: If you have existing data that should be organized:
-- Example: Link stories to epics based on naming convention or manual mapping
-- This is project-specific and should be customized

-- Step 4: Verify constraints
-- Check for any orphaned references
SELECT * FROM task WHERE parent_task_id IS NOT NULL 
  AND parent_task_id NOT IN (SELECT id FROM task);

SELECT * FROM task WHERE epic_id IS NOT NULL 
  AND epic_id NOT IN (SELECT id FROM task WHERE type = 'EPIC');
```

## 9. Testing Requirements

### 9.1 Unit Tests

- Test hierarchy validation rules
- Test circular reference prevention
- Test cascade deletion behavior
- Test status change validations

### 9.2 Integration Tests

- Test creating epic with child issues
- Test creating subtask with parent
- Test moving issues between epics
- Test deleting tasks with hierarchy
- Test querying hierarchy relationships

### 9.3 Test Data Setup

```java
@TestConfiguration
public class HierarchyTestData {
    
    public Task createEpicWithChildren() {
        Task epic = new Task();
        epic.setType(TaskType.EPIC);
        epic.setTitle("User Management System");
        epic = taskRepository.save(epic);
        
        Task story = new Task();
        story.setType(TaskType.STORY);
        story.setTitle("Implement user authentication");
        story.setEpic(epic);
        story = taskRepository.save(story);
        
        Task subtask = new Task();
        subtask.setType(TaskType.SUBTASK);
        subtask.setTitle("Create login form");
        subtask.setParentTask(story);
        subtask = taskRepository.save(subtask);
        
        return epic;
    }
}
```

## 10. Frontend Integration Points

### 10.1 API Changes Frontend Must Adopt

Once backend is implemented, frontend should:

1. Remove mock data from `lib/mockHierarchy.ts`
2. Update API calls to fetch real hierarchy data
3. Fetch epics list from `/api/projects/{projectId}/epics`
4. Fetch potential parents from `/api/projects/{projectId}/potential-parents`
5. Include `parentTaskId` and `epicId` in create/update requests
6. Handle hierarchy validation errors from backend

### 10.2 API Response Example

```json
{
  "id": 1001,
  "title": "Implement user authentication",
  "type": "STORY",
  "status": "IN_PROGRESS",
  "parentTaskId": null,
  "epicId": 2001,
  "epic": {
    "id": 2001,
    "title": "User Management System",
    "type": "EPIC",
    "status": "IN_PROGRESS"
  },
  "subtasks": [
    {
      "id": 3001,
      "title": "Create login form",
      "type": "SUBTASK",
      "status": "DONE"
    },
    {
      "id": 3002,
      "title": "Add JWT authentication",
      "type": "SUBTASK",
      "status": "IN_PROGRESS"
    }
  ]
}
```

## 11. Performance Considerations

### 11.1 Lazy Loading

- Use `@ManyToOne(fetch = FetchType.LAZY)` for parent and epic relationships
- Only fetch subtasks when needed (not in list queries)
- Consider using DTO projections for list views

### 11.2 Caching

- Cache epic lists per project (they change infrequently)
- Cache hierarchy counts (number of subtasks)
- Invalidate cache when hierarchy changes

### 11.3 Query Optimization

```java
// Use JOIN FETCH to avoid N+1 queries
@Query("SELECT t FROM Task t LEFT JOIN FETCH t.parentTask LEFT JOIN FETCH t.epic WHERE t.id = :id")
Optional<Task> findByIdWithHierarchy(@Param("id") Long id);

// Batch fetch subtasks
@Query("SELECT t FROM Task t WHERE t.parentTask.id IN :parentIds")
List<Task> findSubtasksByParentIds(@Param("parentIds") List<Long> parentIds);
```

## 12. Implementation Phases

### Phase 1: Core Database & Model (Week 1)
- [ ] Update database schema
- [ ] Update entity models
- [ ] Add basic repository methods
- [ ] Write unit tests for models

### Phase 2: Service Layer (Week 1-2)
- [ ] Implement hierarchy validation
- [ ] Update TaskService methods
- [ ] Add new service methods for hierarchy
- [ ] Write service layer tests

### Phase 3: API Layer (Week 2)
- [ ] Update existing endpoints
- [ ] Add new hierarchy endpoints
- [ ] Update DTOs
- [ ] Write API integration tests

### Phase 4: Testing & Migration (Week 2-3)
- [ ] Run full test suite
- [ ] Prepare data migration scripts
- [ ] Test with production-like data
- [ ] Performance testing

### Phase 5: Deployment (Week 3)
- [ ] Deploy to staging
- [ ] Frontend integration testing
- [ ] User acceptance testing
- [ ] Production deployment

## 13. Rollback Plan

If issues arise:

1. Revert database migration (use backup)
2. Revert backend code changes
3. Frontend will fall back to mock data automatically
4. Investigate and fix issues
5. Redeploy when ready

## 14. Documentation Updates

After implementation:

- [ ] Update API documentation (Swagger/OpenAPI)
- [ ] Update database schema documentation
- [ ] Create user guide for issue hierarchy
- [ ] Update developer onboarding docs

## 15. Success Metrics

- All tests passing (>95% coverage)
- API response time < 200ms for hierarchy queries
- No circular reference bugs reported
- Frontend successfully integrated
- Zero data loss during migration

---

**Next Steps:**
1. Review and approve this plan
2. Set up development branch: `feature/issue-hierarchy`
3. Begin Phase 1 implementation
4. Schedule code review after each phase
