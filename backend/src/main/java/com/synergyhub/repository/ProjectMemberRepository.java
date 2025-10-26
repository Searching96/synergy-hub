package com.synergyhub.repository;

import com.synergyhub.domain.entity.ProjectMember;
import com.synergyhub.domain.entity.ProjectMember.ProjectMemberId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectMemberRepository extends JpaRepository<ProjectMember, ProjectMemberId> {

    // Find all members of a project
    List<ProjectMember> findByProjectId(Integer projectId);

    List<ProjectMember> findByUserId(Integer userId);

    boolean existsByProjectIdAndUserId(Integer projectId, Integer userId);

    @Query("SELECT pm FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    Optional<ProjectMember> findByProjectIdAndUserId(@Param("projectId") Integer projectId, @Param("userId") Integer userId);

    List<ProjectMember> findByProjectIdAndRole(Integer projectId, String role);

    void deleteByProjectIdAndUserId(Integer projectId, Integer userId);

    long countByProjectId(Integer projectId);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END " +
            "FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    boolean hasAccessToTaskProject(@Param("projectId") Integer projectId, @Param("userId") Integer userId);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END " +
            "FROM Project p WHERE p.id = :projectId AND p.projectLead.id = :userId")
    boolean isProjectLead(@Param("projectId") Integer projectId, @Param("userId") Integer userId);

    @Query("SELECT pm.role FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId")
    Optional<String> getUserRoleInProject(@Param("projectId") Integer projectId, @Param("userId") Integer userId);

    @Query("SELECT CASE WHEN COUNT(pm) > 0 THEN true ELSE false END " +
            "FROM ProjectMember pm WHERE pm.project.id = :projectId AND pm.user.id = :userId " +
            "AND pm.role IN :allowedRoles")
    boolean hasRoleInProject(@Param("projectId") Integer projectId,
                             @Param("userId") Integer userId,
                             @Param("allowedRoles") List<String> allowedRoles);
}
