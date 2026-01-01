package com.synergyhub.repository;

import com.synergyhub.domain.entity.AuditLog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Integer> {

    // ✅ FIX: Added the method used by ActivityStreamService
    // Using @Query allows us to fetch logs for the project, ordered by time
    @Query("SELECT a FROM AuditLog a WHERE a.projectId = :projectId ORDER BY a.timestamp DESC")
    Page<AuditLog> findProjectActivity(@Param("projectId") Integer projectId, Pageable pageable);

    // Find by user
    List<AuditLog> findByUserId(Integer userId, Pageable pageable);
    
    // Find by project
    List<AuditLog> findByProjectId(Integer projectId, Pageable pageable);
    
    // Find by user and time range
    List<AuditLog> findByUserIdAndTimestampAfter(Integer userId, LocalDateTime after);
    
    // Find by event type (for filtering)
    List<AuditLog> findByEventType(String eventType, Pageable pageable);
    
    // Find by IP address (security investigation)
    List<AuditLog> findByIpAddress(String ipAddress, Pageable pageable);
    
    // Find system events (no user)
    List<AuditLog> findByUserIsNull(Pageable pageable);
    
    // Find in date range
    List<AuditLog> findByTimestampBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}