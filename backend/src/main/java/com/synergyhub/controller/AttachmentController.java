package com.synergyhub.controller;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.response.AttachmentResponse;
import com.synergyhub.service.attachments.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.synergyhub.security.UserPrincipal;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/tasks/{taskId}/attachments")
@RequiredArgsConstructor
@Slf4j
public class AttachmentController {

    private final AttachmentService attachmentService;

    /**
     * Upload file attachment to a task
     */
    @PostMapping
    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #principal.user)")
    public ResponseEntity<AttachmentResponse> uploadAttachment(
            @PathVariable Long taskId,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            log.info("Uploading attachment for task: {}", taskId);
            
            AttachmentResponse response = attachmentService.uploadFile(taskId, file, principal.getUser());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid file: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error uploading file", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get all attachments for a task
     */
    @GetMapping
    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #principal.user)")
    public ResponseEntity<List<AttachmentResponse>> getTaskAttachments(
            @PathVariable Long taskId,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            log.info("Fetching attachments for task: {}", taskId);
            
            List<AttachmentResponse> attachments = attachmentService.getTaskAttachments(taskId);
            
            return ResponseEntity.ok(attachments);
        } catch (Exception e) {
            log.error("Error fetching attachments", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete attachment
     */
    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #principal.user)")
    public ResponseEntity<Void> deleteAttachment(
            @PathVariable Long taskId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            log.info("Deleting attachment: {} from task: {}", attachmentId, taskId);
            
            attachmentService.deleteAttachment(attachmentId, principal.getUser());
            
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error deleting attachment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Get download URL for attachment
     */
    @GetMapping("/{attachmentId}/download-url")
    @PreAuthorize("@projectSecurity.hasTaskAccess(#taskId, #principal.user)")
    public ResponseEntity<String> getDownloadUrl(
            @PathVariable Long taskId,
            @PathVariable Long attachmentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        try {
            log.info("Generating download URL for attachment: {}", attachmentId);
            
            String downloadUrl = attachmentService.getDownloadUrl(attachmentId, principal.getUser());
            
            return ResponseEntity.ok(downloadUrl);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid request: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error generating download URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
