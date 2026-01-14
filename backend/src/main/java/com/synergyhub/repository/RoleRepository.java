package com.synergyhub.repository;

import com.synergyhub.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByNameAndOrganizationId(String name, Long organizationId);

    @Query("SELECT r FROM Role r JOIN FETCH r.permissions WHERE r.name = :name AND r.organization.id = :organizationId")
    Optional<Role> findByNameWithPermissions(@Param("name") String name, @Param("organizationId") Long organizationId);

    boolean existsByNameAndOrganizationId(String name, Long organizationId);

    List<Role> findByOrganizationId(Long organizationId);
}