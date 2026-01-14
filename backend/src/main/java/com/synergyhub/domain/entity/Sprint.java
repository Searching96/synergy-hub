package com.synergyhub.domain.entity;

import com.synergyhub.domain.enums.SprintStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sprints")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(exclude = {"project", "tasks"})
public class Sprint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sprint_id")
    @EqualsAndHashCode.Include
    private Long id;

    @NotNull(message = "Project is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @NotBlank(message = "Sprint name is required")
    @Size(max = 100, message = "Sprint name must not exceed 100 characters")
    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String goal;

    @NotNull(message = "Start date is required")
    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @NotNull(message = "Sprint status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SprintStatus status = SprintStatus.PLANNING;

    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
    @BatchSize(size = 20)
    @Builder.Default
    private List<Task> tasks = new ArrayList<>();

    // Helper methods for bidirectional relationship
    public void addTask(Task task) {
        tasks.add(task);
        task.setSprint(this);
    }

    public void removeTask(Task task) {
        tasks.remove(task);
        task.setSprint(null);
    }

    // Business logic methods
    public boolean canBeStarted() {
        return status == SprintStatus.PLANNING;
    }

    public boolean canBeCompleted() {
        return status == SprintStatus.ACTIVE;
    }

    public boolean canBeDeleted() {
        return status == SprintStatus.PLANNING || status == SprintStatus.CANCELLED;
    }

    public boolean isActive() {
        return status == SprintStatus.ACTIVE;
    }

    @AssertTrue(message = "End date must be after start date")
    public boolean isValidDateRange() {
        return startDate == null || endDate == null || !endDate.isBefore(startDate);
    }
}
