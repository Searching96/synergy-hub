package com.synergyhub.repository;

import com.synergyhub.domain.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Public APIs (used before organization context is set)
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"memberships", "memberships.role", "memberships.role.permissions", "memberships.organization"})
    @Query("SELECT u FROM User u WHERE u.email = :email")
    Optional<User> findByEmailWithRolesAndPermissions(@Param("email") String email);
    
    @EntityGraph(attributePaths = {"memberships", "memberships.role", "memberships.role.permissions", "memberships.organization"})
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdWithRolesAndPermissions(@Param("id") Long id);
    
    // Organization-scoped APIs (require organization context)
    @Query("SELECT u FROM User u WHERE u.email = :email AND u IN (SELECT uo.user FROM UserOrganization uo WHERE uo.organization.id = :orgId)")
    Optional<User> findByEmailInOrganization(@Param("email") String email, @Param("orgId") Long orgId);
    
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM User u WHERE u.email = :email AND u IN (SELECT uo.user FROM UserOrganization uo WHERE uo.organization.id = :orgId)")
    boolean existsByEmailInOrganization(@Param("email") String email, @Param("orgId") Long orgId);

    @EntityGraph(attributePaths = {"memberships", "memberships.role", "memberships.role.permissions", "memberships.organization"})
    @Query("SELECT u FROM User u WHERE u.email = :email AND u IN (SELECT uo.user FROM UserOrganization uo WHERE uo.organization.id = :orgId)")
    Optional<User> findByEmailWithRolesAndPermissionsInOrganization(@Param("email") String email, @Param("orgId") Long orgId);
    
    @EntityGraph(attributePaths = {"memberships", "memberships.role", "memberships.role.permissions", "memberships.organization"})
    @Query("SELECT u FROM User u WHERE u.id = :id AND u IN (SELECT uo.user FROM UserOrganization uo WHERE uo.organization.id = :orgId)")
    Optional<User> findByIdWithRolesAndPermissionsInOrganization(@Param("id") Long id, @Param("orgId") Long orgId);
    
    @Query("SELECT u FROM User u WHERE u IN (SELECT uo.user FROM UserOrganization uo WHERE uo.organization.id = :organizationId)")
    List<User> findByOrganizationId(@Param("organizationId") Long organizationId);
    
    @Query("SELECT u FROM User u WHERE u.accountLocked = true AND u.lockUntil < :now AND u IN (SELECT uo.user FROM UserOrganization uo WHERE uo.organization.id = :orgId)")
    List<User> findExpiredLockedAccountsInOrganization(@Param("now") LocalDateTime now, @Param("orgId") Long orgId);
    
    @Query("SELECT COUNT(u) FROM User u WHERE u IN (SELECT uo.user FROM UserOrganization uo WHERE uo.organization.id = :orgId)")
    long countByOrganizationId(@Param("orgId") Long orgId);

    @Modifying(flushAutomatically = false, clearAutomatically = false)
    @Query("UPDATE User u SET u.emailVerified = TRUE WHERE u.id = :id")
    int markEmailVerified(@Param("id") Long id);

    @Query("UPDATE User u SET u.emailVerified = TRUE WHERE u.id = :id AND u IN (SELECT uo.user FROM UserOrganization uo WHERE uo.organization.id = :orgId)")
    int markEmailVerifiedInOrganization(@Param("id") Long id, @Param("orgId") Long orgId);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.memberships m LEFT JOIN FETCH m.organization WHERE u.id = :userId")
    Optional<User> findByIdWithOrganization(@Param("userId") Long userId);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.memberships m LEFT JOIN FETCH m.organization WHERE u.id = :userId AND u IN (SELECT uo.user FROM UserOrganization uo WHERE uo.organization.id = :orgId)")
    Optional<User> findByIdWithOrganizationInOrganization(@Param("userId") Long userId, @Param("orgId") Long orgId);
}