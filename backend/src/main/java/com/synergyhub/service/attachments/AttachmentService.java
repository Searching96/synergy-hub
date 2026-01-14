package com.synergyhub.service.attachments;

import com.synergyhub.domain.entity.Attachment;
import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.response.AttachmentResponse;
import com.synergyhub.repository.AttachmentRepository;
import com.synergyhub.config.MinioProperties;
import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private final AttachmentRepository attachmentRepository;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    /**
     * Initialize MinIO buckets on application startup
     */
    @jakarta.annotation.PostConstruct
    public void initializeBuckets() {
        try {
            String[] buckets = {
                minioProperties.getBucket().getAttachments(),
                minioProperties.getBucket().getAvatars(),
                minioProperties.getBucket().getTemp()
            };

            for (String bucket : buckets) {
                if (bucket != null && !bucket.isBlank()) {
                    boolean exists = minioClient.bucketExists(
                        BucketExistsArgs.builder().bucket(bucket).build()
                    );
                    if (!exists) {
                        minioClient.makeBucket(
                            MakeBucketArgs.builder().bucket(bucket).build()
                        );
                        log.info("Created MinIO bucket: {}", bucket);
                    } else {
                        log.debug("MinIO bucket already exists: {}", bucket);
                    }
                }
            }
            log.info("MinIO buckets initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize MinIO buckets: {}", e.getMessage(), e);
            // Don't fail startup - buckets may be created manually
        }
    }

    /**
     * Upload a file and save attachment metadata
     */
    @Transactional
    public AttachmentResponse uploadFile(Long taskId, MultipartFile file, User uploader) throws Exception {
        log.info("Uploading file: {} for task: {}", file.getOriginalFilename(), taskId);

        // Validate file
        validateFile(file);

        // Generate unique file key
        String fileKey = generateFileKey(taskId, file.getOriginalFilename());
        String bucketName = minioProperties.getBucket().getAttachments();

        // Upload to MinIO
        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );
        }

        // Generate file URL
        String fileUrl = generateFileUrl(bucketName, fileKey);

        // Create attachment entity
        Attachment attachment = Attachment.builder()
                .taskId(taskId)
                .fileName(file.getOriginalFilename())
                .fileSize(file.getSize())
                .fileType(file.getContentType())
                .fileKey(fileKey)
                .fileUrl(fileUrl)
                .bucketName(bucketName)
                .uploadedBy(uploader.getId())
                .uploadedAt(LocalDateTime.now())
                .deleted(false)
                .build();

        Attachment saved = attachmentRepository.save(attachment);
        
        log.info("File uploaded successfully: {} (ID: {})", file.getOriginalFilename(), saved.getId());
        
        return mapToResponse(saved, uploader.getName());
    }

    /**
     * Get all attachments for a task
     */
    @Transactional(readOnly = true)
    public List<AttachmentResponse> getTaskAttachments(Long taskId) {
        log.info("Fetching attachments for task: {}", taskId);
        
        return attachmentRepository.findByTaskId(taskId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete an attachment (soft delete)
     */
    @Transactional
    public void deleteAttachment(Long attachmentId, User currentUser) throws Exception {
        log.info("Deleting attachment: {}", attachmentId);

        Attachment attachment = attachmentRepository.findByIdActive(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));

        // Verify ownership or admin privileges
        if (!attachment.getUploadedBy().equals(currentUser.getId())) {
            throw new IllegalArgumentException("User does not have permission to delete this attachment");
        }

        // Soft delete
        attachment.setDeleted(true);
        attachment.setDeletedAt(LocalDateTime.now());
        attachmentRepository.save(attachment);

        // Optionally delete from MinIO (commented for safety)
        // deleteFromMinIO(attachment.getBucketName(), attachment.getFileKey());

        log.info("Attachment deleted: {}", attachmentId);
    }

    /**
     * Download attachment - generate presigned URL
     */
    public String getDownloadUrl(Long attachmentId, User currentUser) throws Exception {
        log.info("Generating download URL for attachment: {}", attachmentId);

        Attachment attachment = attachmentRepository.findByIdActive(attachmentId)
                .orElseThrow(() -> new IllegalArgumentException("Attachment not found"));

        // Generate presigned URL (default expiration: 7 days)
        String presignedUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                        .method(Method.GET)
                        .bucket(attachment.getBucketName())
                        .object(attachment.getFileKey())
                        .build()
        );

        log.info("Download URL generated for attachment: {}", attachmentId);
        return presignedUrl;
    }

    /**
     * Validate file before upload
     */
    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size
        long maxSize = parseFileSize(minioProperties.getMaxFileSize());
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds maximum allowed: " + minioProperties.getMaxFileSize());
        }

        // Check file extension
        String fileName = file.getOriginalFilename();
        if (fileName == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = getFileExtension(fileName).toLowerCase();
        if (!minioProperties.getAllowedExtensions().contains(extension)) {
            throw new IllegalArgumentException("File type not allowed: " + extension);
        }
    }

    /**
     * Generate unique file key (S3 object key)
     */
    private String generateFileKey(Long taskId, String originalFileName) {
        String timestamp = LocalDateTime.now().toString().replace(":", "-");
        String extension = getFileExtension(originalFileName);
        String uniqueId = UUID.randomUUID().toString();
        
        return String.format("tasks/%d/%s_%s.%s", taskId, timestamp, uniqueId, extension);
    }

    /**
     * Generate file URL
     */
    private String generateFileUrl(String bucket, String fileKey) {
        return minioProperties.getPublicUrl().replaceAll("/$", "") + "/" + bucket + "/" + fileKey;
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }

    /**
     * Parse file size string (e.g., "10MB" -> bytes)
     */
    private long parseFileSize(String sizeStr) {
        if (sizeStr == null || sizeStr.isBlank()) {
            return 10 * 1024 * 1024; // Default 10MB
        }

        String[] parts = sizeStr.toUpperCase().replaceAll("[\\s,]", "").split("(?=B)");
        if (parts.length == 0) return 10 * 1024 * 1024;

        long size = Long.parseLong(parts[0]);
        String unit = parts.length > 1 ? parts[1] : "B";

        return switch (unit) {
            case "B" -> size;
            case "KB" -> size * 1024;
            case "MB" -> size * 1024 * 1024;
            case "GB" -> size * 1024 * 1024 * 1024;
            default -> 10 * 1024 * 1024;
        };
    }

    /**
     * Map attachment to response DTO
     */
    private AttachmentResponse mapToResponse(Attachment attachment) {
        return mapToResponse(attachment, attachment.getUploader() != null ? attachment.getUploader().getName() : "Unknown");
    }

    private AttachmentResponse mapToResponse(Attachment attachment, String uploaderName) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .taskId(attachment.getTaskId())
                .fileName(attachment.getFileName())
                .fileSize(attachment.getFileSize())
                .fileType(attachment.getFileType())
                .fileUrl(attachment.getFileUrl())
                .thumbnailUrl(attachment.getThumbnailUrl())
                .bucketName(attachment.getBucketName())
                .uploadedBy(attachment.getUploadedBy())
                .uploaderName(uploaderName)
                .uploadedAt(attachment.getUploadedAt())
                .build();
    }

    /**
     * Delete file from MinIO
     */
    private void deleteFromMinIO(String bucket, String fileKey) throws Exception {
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileKey)
                            .build()
            );
            log.info("File deleted from MinIO: {}/{}", bucket, fileKey);
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO: {}/{}", bucket, fileKey, e);
            throw e;
        }
    }
}
