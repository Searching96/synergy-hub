package com.synergyhub.repository;

import com.synergyhub.domain.entity.Sprint;
import com.synergyhub.domain.enums.SprintStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SprintRepository extends JpaRepository<Sprint, Long> {

    // Find all sprints in a project
    List<Sprint> findByProjectIdOrderByStartDateDesc(Long projectId);

    // Find active sprint in project
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.status = 'ACTIVE'")
    Optional<Sprint> findActiveSprintByProjectId(@Param("projectId") Long projectId);

    // Find sprints by status
    List<Sprint> findByProjectIdAndStatus(Long projectId, SprintStatus status);

    // Find upcoming sprints
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.startDate > :date AND s.status = 'PLANNING' ORDER BY s.startDate ASC")
    List<Sprint> findUpcomingSprintsByProjectId(@Param("projectId") Long projectId, @Param("date") LocalDate date);

    // Find completed sprints
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.status = 'COMPLETED' ORDER BY s.endDate DESC")
    List<Sprint> findCompletedSprintsByProjectId(@Param("projectId") Long projectId);

    // Check if sprint name exists in project
    boolean existsByProjectIdAndName(Long projectId, String name);

    // Find sprint with tasks
        @Query("SELECT s FROM Sprint s LEFT JOIN FETCH s.tasks WHERE s.id = :id")
        Optional<Sprint> findByIdWithTasks(@Param("id") Long id);

    // Count sprints by project
    long countByProjectId(Long projectId);

    // Find overlapping sprints
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId " +
            "AND s.status IN ('ACTIVE', 'PLANNING') " +
            "AND ((s.startDate <= :endDate AND s.endDate >= :startDate))")
    List<Sprint> findOverlappingSprints(@Param("projectId") Long projectId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}
