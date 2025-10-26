package com.synergyhub.domain.entity;

import com.synergyhub.domain.enums.SprintStatus;
import jakarta.persistence.*;
import lombok.*;

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
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String goal;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SprintStatus status = SprintStatus.PLANNING;

    @OneToMany(mappedBy = "sprint", cascade = CascadeType.ALL, orphanRemoval = true)
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
}
