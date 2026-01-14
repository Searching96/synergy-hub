package com.synergyhub.repository;

import com.synergyhub.domain.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    // Find non-deleted attachments by task ID
    @Query("SELECT a FROM Attachment a WHERE a.taskId = :taskId AND a.deleted = false ORDER BY a.uploadedAt DESC")
    List<Attachment> findByTaskId(@Param("taskId") Long taskId);

    // Find attachment by ID and ensure it's not deleted
    @Query("SELECT a FROM Attachment a WHERE a.id = :id AND a.deleted = false")
    java.util.Optional<Attachment> findByIdActive(@Param("id") Long id);

    // Count non-deleted attachments for a task
    @Query("SELECT COUNT(a) FROM Attachment a WHERE a.taskId = :taskId AND a.deleted = false")
    int countByTaskId(@Param("taskId") Long taskId);

    // Find by file key (for quick lookup)
    @Query("SELECT a FROM Attachment a WHERE a.fileKey = :fileKey AND a.deleted = false")
    java.util.Optional<Attachment> findByFileKey(@Param("fileKey") String fileKey);
}
