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
public interface SprintRepository extends JpaRepository<Sprint, Integer> {

    // Find all sprints in a project
    List<Sprint> findByProjectIdOrderByStartDateDesc(Integer projectId);

    // Find active sprint in project
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.status = 'ACTIVE'")
    Optional<Sprint> findActiveSprintByProjectId(@Param("projectId") Integer projectId);

    // Find sprints by status
    List<Sprint> findByProjectIdAndStatus(Integer projectId, SprintStatus status);

    // Find upcoming sprints
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.startDate > :date AND s.status = 'PLANNING' ORDER BY s.startDate ASC")
    List<Sprint> findUpcomingSprintsByProjectId(@Param("projectId") Integer projectId, @Param("date") LocalDate date);

    // Find completed sprints
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId AND s.status = 'COMPLETED' ORDER BY s.endDate DESC")
    List<Sprint> findCompletedSprintsByProjectId(@Param("projectId") Integer projectId);

    // Check if sprint name exists in project
    boolean existsByProjectIdAndName(Integer projectId, String name);

    // Find sprint with tasks
    @Query("SELECT s FROM Sprint s LEFT JOIN FETCH s.tasks WHERE s.id = :id")
    Optional<Sprint> findByIdWithTasks(@Param("id") Integer id);

    // Count sprints by project
    long countByProjectId(Integer projectId);

    // Find overlapping sprints
    @Query("SELECT s FROM Sprint s WHERE s.project.id = :projectId " +
            "AND s.status IN ('ACTIVE', 'PLANNING') " +
            "AND ((s.startDate <= :endDate AND s.endDate >= :startDate))")
    List<Sprint> findOverlappingSprints(@Param("projectId") Integer projectId,
                                        @Param("startDate") LocalDate startDate,
                                        @Param("endDate") LocalDate endDate);
}
