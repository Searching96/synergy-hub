package com.synergyhub.domain.entity;

import com.synergyhub.domain.enums.TaskPriority;
import com.synergyhub.domain.enums.TaskStatus;
import com.synergyhub.domain.enums.TaskType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tasks", indexes = {
        @Index(name = "idx_task_sprint", columnList = "sprint_id"),
        @Index(name = "idx_task_assignee", columnList = "assignee_id"),
        @Index(name = "idx_task_project", columnList = "project_id"),
        @Index(name = "idx_task_status", columnList = "status"),
        @Index(name = "idx_task_parent", columnList = "parent_task_id"),
        @Index(name = "idx_task_reporter", columnList = "reporter_id") // ✅ Add index for reporter queries
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"sprint", "project", "assignee", "reporter", "parentTask", "subtasks"}) // ✅ FIXED: Changed "creator" to "reporter"
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "task_id")
    @EqualsAndHashCode.Include
    private Integer id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id")
    private Sprint sprint;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_task_id")
    private Task parentTask;

    @OneToMany(mappedBy = "parentTask", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Task> subtasks = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskStatus status = TaskStatus.TO_DO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskPriority priority = TaskPriority.MEDIUM;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TaskType type = TaskType.TASK;

    /**
     * The user who created/reported this task.
     * In Agile terminology, this is the "Reporter" - the person who discovered
     * or reported the issue. This may be different from the assignee.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(name = "story_points")
    private Integer storyPoints;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ========== Business Logic Methods ==========
    
    public boolean canBeAssigned() {
        return status != TaskStatus.DONE && status != TaskStatus.CANCELLED;
    }

    public boolean canBeMovedToSprint() {
        return status == TaskStatus.TO_DO || status == TaskStatus.BACKLOG;
    }

    public boolean isOverdue() {
        return dueDate != null &&
                LocalDateTime.now().isAfter(dueDate) &&
                status != TaskStatus.DONE;
    }

    public boolean isInProgress() {
        return status == TaskStatus.IN_PROGRESS || status == TaskStatus.IN_REVIEW;
    }
    
    public boolean isSubtask() {
        return parentTask != null;
    }

    // ✅ Optional: Convenience method if you prefer "creator" terminology elsewhere
    public User getCreator() {
        return reporter;
    }

    // ✅ Check if task is assigned to someone
    public boolean isAssigned() {
        return assignee != null;
    }

    // ✅ Check if current user is the reporter
    public boolean isReportedBy(User user) {
        return reporter != null && reporter.equals(user);
    }

    // ✅ Check if current user is the assignee
    public boolean isAssignedTo(User user) {
        return assignee != null && assignee.equals(user);
    }

    // ✅ Check if task can be deleted (business rule)
    public boolean canBeDeleted() {
        // Tasks in progress or done with subtasks shouldn't be easily deleted
        return !(isInProgress() && !subtasks.isEmpty());
    }

    // ✅ Helper to add subtask with automatic parent reference
    public void addSubtask(Task subtask) {
        subtasks.add(subtask);
        subtask.setParentTask(this);
        subtask.setProject(this.project); // Subtasks inherit parent's project
    }

    // ✅ Helper to remove subtask
    public void removeSubtask(Task subtask) {
        subtasks.remove(subtask);
        subtask.setParentTask(null);
    }

    // ✅ Check if all subtasks are completed
    public boolean areAllSubtasksCompleted() {
        return subtasks.isEmpty() || 
               subtasks.stream().allMatch(t -> t.getStatus() == TaskStatus.DONE);
    }

    // ✅ Get completion percentage based on subtasks
    public double getCompletionPercentage() {
        if (subtasks.isEmpty()) {
            return status == TaskStatus.DONE ? 100.0 : 0.0;
        }
        
        long completedSubtasks = subtasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.DONE)
                .count();
        
        return (completedSubtasks * 100.0) / subtasks.size();
    }
}