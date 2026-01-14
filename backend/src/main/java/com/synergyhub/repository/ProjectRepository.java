package com.synergyhub.repository;

import com.synergyhub.domain.entity.Project;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    // Find all projects in an organization
    List<Project> findByOrganizationId(Long organizationId);

    // Find project by name within an organization
    Optional<Project> findByNameAndOrganizationId(String name, Long organizationId);

    // Find all projects led by a user
    List<Project> findByProjectLeadId(Long projectLeadId);

    // Check if project name exists in organization
    boolean existsByNameAndOrganizationId(String name, Long organizationId);

    // Find project by id and organization
    Optional<Project> findByIdAndOrganizationId(Long id, Long organizationId);

    // Find project with members eagerly loaded
    @EntityGraph(attributePaths = {"projectMembers", "projectMembers.user", "projectLead", "organization"})
    @Query("SELECT p FROM Project p WHERE p.id = :id")
    Optional<Project> findByIdWithMembers(@Param("id") Long id);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.projectMembers m " +
       "WHERE p.projectLead.id = :userId OR m.user.id = :userId")
    List<Project> findProjectsForUser(@Param("userId") Long userId);

    // Count projects by organization
    long countByOrganizationId(Long organizationId);

    // Find projects by status
    List<Project> findByStatusAndOrganizationId(String status, Long organizationId);

    // Find all active projects in organization
    @Query("SELECT p FROM Project p WHERE p.organization.id = :organizationId AND p.status = 'ACTIVE'")
    List<Project> findActiveProjectsByOrganization(@Param("organizationId") Long organizationId);

    // Advanced search for projects where user is member or lead
    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.projectMembers m " +
           "WHERE (p.projectLead.id = :userId OR m.user.id = :userId) " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "AND (:status IS NULL OR p.status = :status)")
    org.springframework.data.domain.Page<Project> findProjectsForUserWithFilter(
            @Param("userId") Long userId,
            @Param("search") String search,
            @Param("status") String status,
            org.springframework.data.domain.Pageable pageable);
}
