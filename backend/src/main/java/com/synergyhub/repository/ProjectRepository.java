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
public interface ProjectRepository extends JpaRepository<Project, Integer> {

    // Find all projects in an organization
    List<Project> findByOrganizationId(Integer organizationId);

    // Find project by name within an organization
    Optional<Project> findByNameAndOrganizationId(String name, Integer organizationId);

    // Find all projects led by a user
    List<Project> findByProjectLeadId(Integer projectLeadId);

    // Check if project name exists in organization
    boolean existsByNameAndOrganizationId(String name, Integer organizationId);

    // Find project by id and organization
    Optional<Project> findByIdAndOrganizationId(Integer id, Integer organizationId);

    // Find project with members eagerly loaded
    @EntityGraph(attributePaths = {"projectMembers", "projectMembers.user", "projectLead", "organization"})
    @Query("SELECT p FROM Project p WHERE p.id = :id")
    Optional<Project> findByIdWithMembers(@Param("id") Integer id);

    @Query("SELECT DISTINCT p FROM Project p LEFT JOIN p.projectMembers m " +
       "WHERE p.projectLead.id = :userId OR m.user.id = :userId")
    List<Project> findProjectsForUser(@Param("userId") Integer userId);

    // Count projects by organization
    long countByOrganizationId(Integer organizationId);

    // Find projects by status
    List<Project> findByStatusAndOrganizationId(String status, Integer organizationId);

    // Find all active projects in organization
    @Query("SELECT p FROM Project p WHERE p.organization.id = :organizationId AND p.status = 'ACTIVE'")
    List<Project> findActiveProjectsByOrganization(@Param("organizationId") Integer organizationId);
}
