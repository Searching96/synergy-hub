package com.synergyhub.auth.repository;

import com.synergyhub.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    @Query("SELECT u FROM User u WHERE u.email = :email AND u.accountLocked = false")
    Optional<User> findActiveUserByEmail(@Param("email") String email);
    
    @Query("SELECT u FROM User u WHERE u.lockUntil IS NOT NULL AND u.lockUntil < :now")
    List<User> findUsersWithExpiredLocks(@Param("now") LocalDateTime now);
    
    List<User> findByOrganizationId(Integer organizationId);
}