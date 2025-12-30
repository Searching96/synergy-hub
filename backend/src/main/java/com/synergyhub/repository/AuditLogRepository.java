package com.synergyhub.repository;

import com.synergyhub.domain.entity.AuditLog;
import com.synergyhub.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    Page<AuditLog> findByUser(User user, Pageable pageable);
    
    Page<AuditLog> findByEventType(String eventType, Pageable pageable);
    
    @Query("SELECT a FROM AuditLog a WHERE a.user = :user AND a.createdAt BETWEEN :start AND :end ORDER BY a.createdAt DESC")
    List<AuditLog> findByUserAndDateRange(
        @Param("user") User user,
        @Param("start") LocalDateTime start,
        @Param("end") LocalDateTime end
    );
    
    @Query("SELECT a FROM AuditLog a WHERE a.eventType = :eventType AND a.createdAt > :since ORDER BY a.createdAt DESC")
    List<AuditLog> findRecentByEventType(@Param("eventType") String eventType, @Param("since") LocalDateTime since);

    // ✅ NEW: Required for Activity Stream
    // This finds all actions performed by users who are members of the given project.
    // (This is an MVP approach. For stricter data, you'd add a 'projectId' column to AuditLog directly)
    @Query("SELECT a FROM AuditLog a WHERE a.user.id IN " +
           "(SELECT m.user.id FROM ProjectMember m WHERE m.project.id = :projectId) " +
           "ORDER BY a.createdAt DESC")
    Page<AuditLog> findProjectActivity(@Param("projectId") Integer projectId, Pageable pageable);
}