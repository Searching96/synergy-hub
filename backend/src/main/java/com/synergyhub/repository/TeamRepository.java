package com.synergyhub.repository;

import com.synergyhub.domain.entity.Team;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long> {
    List<Team> findByOrganizationId(Long organizationId);
    
    @Query("SELECT t FROM Team t WHERE t.id = :teamId AND t.organization.id = :orgId")
    Optional<Team> findByIdInOrganization(@Param("teamId") Long teamId, @Param("orgId") Long orgId);
    
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Team t WHERE t.id = :teamId AND t.organization.id = :orgId")
    boolean existsByIdInOrganization(@Param("teamId") Long teamId, @Param("orgId") Long orgId);
}
