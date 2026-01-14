# S3 File Storage Implementation Plan (On-Premise VPS)

**Date:** January 6, 2026  
**Status:** Planning  
**Priority:** High

## Overview

This document outlines the implementation plan for S3-compatible file storage on an on-premise VPS using MinIO. MinIO is an open-source, S3-compatible object storage server that can be self-hosted, making it ideal for on-premise deployments.

## 1. Architecture Overview

### 1.1 Components

```
┌─────────────┐      ┌──────────────┐      ┌──────────────┐
│   Frontend  │─────>│    Backend   │─────>│    MinIO     │
│   (React)   │      │    (Java)    │      │  (S3 API)    │
└─────────────┘      └──────────────┘      └──────────────┘
                              │
                              ▼
                     ┌──────────────┐
                     │   PostgreSQL │
                     │  (Metadata)  │
                     └──────────────┘
```

### 1.2 Data Flow

1. User uploads file via frontend
2. Frontend sends file to backend API
3. Backend uploads file to MinIO
4. MinIO returns file URL
5. Backend stores metadata in PostgreSQL
6. Backend returns attachment info to frontend

## 2. MinIO Installation & Setup

### 2.1 Install MinIO on VPS

**Using Docker (Recommended):**

```bash
# Create MinIO docker-compose.yml
cat > docker-compose.yml <<EOF
version: '3.8'

services:
  minio:
    image: minio/minio:latest
    container_name: synergyhub-minio
    ports:
      - "9000:9000"  # API port
      - "9001:9001"  # Console port
    environment:
      MINIO_ROOT_USER: admin
      MINIO_ROOT_PASSWORD: YourSecurePassword123!
    volumes:
      - minio-data:/data
    command: server /data --console-address ":9001"
    restart: unless-stopped
    networks:
      - synergyhub-network

volumes:
  minio-data:
    driver: local

networks:
  synergyhub-network:
    external: true
EOF

# Start MinIO
docker-compose up -d
```

**Using Binary (Alternative):**

```bash
# Download MinIO binary
wget https://dl.min.io/server/minio/release/linux-amd64/minio
chmod +x minio

# Create data directory
mkdir -p /opt/minio/data

# Run MinIO
MINIO_ROOT_USER=admin MINIO_ROOT_PASSWORD=YourSecurePassword123! \
./minio server /opt/minio/data --console-address ":9001"
```

### 2.2 Configure MinIO

Access MinIO Console at `http://your-vps-ip:9001`

**Create Buckets:**

```bash
# Using MinIO Client (mc)
mc alias set myminio http://localhost:9000 admin YourSecurePassword123!

# Create buckets
mc mb myminio/synergyhub-attachments
mc mb myminio/synergyhub-avatars
mc mb myminio/synergyhub-temp

# Set bucket policies (public read for avatars, private for attachments)
mc policy set download myminio/synergyhub-avatars
mc policy set private myminio/synergyhub-attachments
```

**Configure Bucket Lifecycle (Optional):**

```bash
# Delete temp files after 24 hours
cat > temp-lifecycle.json <<EOF
{
  "Rules": [
    {
      "Expiration": {
        "Days": 1
      },
      "ID": "DeleteTempFiles",
      "Status": "Enabled"
    }
  ]
}
EOF

mc ilm import myminio/synergyhub-temp < temp-lifecycle.json
```

### 2.3 Nginx Reverse Proxy (Optional but Recommended)

```nginx
# /etc/nginx/sites-available/minio
server {
    listen 80;
    server_name files.synergyhub.yourdomain.com;

    # Increase upload size limit
    client_max_body_size 100M;

    location / {
        proxy_pass http://localhost:9000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        # WebSocket support (for console)
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}

# MinIO Console
server {
    listen 80;
    server_name minio-console.synergyhub.yourdomain.com;

    location / {
        proxy_pass http://localhost:9001;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

Enable and reload Nginx:

```bash
sudo ln -s /etc/nginx/sites-available/minio /etc/nginx/sites-enabled/
sudo nginx -t
sudo systemctl reload nginx
```

### 2.4 SSL/TLS Configuration (Recommended)

```bash
# Install Certbot
sudo apt-get install certbot python3-certbot-nginx

# Get SSL certificates
sudo certbot --nginx -d files.synergyhub.yourdomain.com
sudo certbot --nginx -d minio-console.synergyhub.yourdomain.com
```

## 3. Backend Implementation

### 3.1 Add Dependencies (Maven)

```xml
<!-- pom.xml -->
<dependencies>
    <!-- MinIO SDK -->
    <dependency>
        <groupId>io.minio</groupId>
        <artifactId>minio</artifactId>
        <version>8.5.7</version>
    </dependency>
    
    <!-- Apache Tika for file type detection -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
        <version>2.9.1</version>
    </dependency>
    
    <!-- Thumbnailator for image thumbnails -->
    <dependency>
        <groupId>net.coobird</groupId>
        <artifactId>thumbnailator</artifactId>
        <version>0.4.20</version>
    </dependency>
</dependencies>
```

### 3.2 Configuration Properties

```yaml
# application.yml
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:admin}
  secret-key: ${MINIO_SECRET_KEY:YourSecurePassword123!}
  bucket:
    attachments: synergyhub-attachments
    avatars: synergyhub-avatars
    temp: synergyhub-temp
  max-file-size: 10MB # 10 megabytes
  allowed-extensions:
    - jpg
    - jpeg
    - png
    - gif
    - pdf
    - doc
    - docx
    - xls
    - xlsx
    - txt
    - zip
    - rar
  public-url: ${MINIO_PUBLIC_URL:http://localhost:9000}

spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB
```

### 3.3 MinIO Configuration Class

```java
package com.synergyhub.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
```

### 3.4 MinIO Properties Class

```java
package com.synergyhub.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "minio")
public class MinioProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private BucketConfig bucket;
    private String maxFileSize;
    private List<String> allowedExtensions;
    private String publicUrl;

    @Data
    public static class BucketConfig {
        private String attachments;
        private String avatars;
        private String temp;
    }
}
```

## 4. Database Schema

### 4.1 Attachment Table

```sql
CREATE TABLE attachment (
    id BIGSERIAL PRIMARY KEY,
    task_id BIGINT NOT NULL REFERENCES task(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL, -- in bytes
    file_type VARCHAR(100) NOT NULL,
    file_key VARCHAR(500) NOT NULL, -- S3 object key
    file_url TEXT NOT NULL,
    thumbnail_url TEXT,
    bucket_name VARCHAR(100) NOT NULL,
    uploaded_by BIGINT NOT NULL REFERENCES users(id),
    uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted BOOLEAN DEFAULT FALSE,
    deleted_at TIMESTAMP,
    
    CONSTRAINT fk_attachment_task FOREIGN KEY (task_id) REFERENCES task(id),
    CONSTRAINT fk_attachment_user FOREIGN KEY (uploaded_by) REFERENCES users(id)
);

-- Indexes
CREATE INDEX idx_attachment_task_id ON attachment(task_id) WHERE deleted = FALSE;
CREATE INDEX idx_attachment_uploaded_by ON attachment(uploaded_by);
CREATE INDEX idx_attachment_uploaded_at ON attachment(uploaded_at);
```

## 5. Entity & DTO Classes

### 5.1 Attachment Entity

```java
package com.synergyhub.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attachment")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Attachment {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "task_id", nullable = false)
    private Long taskId;
    
    @Column(name = "file_name", nullable = false)
    private String fileName;
    
    @Column(name = "file_size", nullable = false)
    private Long fileSize;
    
    @Column(name = "file_type", nullable = false)
    private String fileType;
    
    @Column(name = "file_key", nullable = false)
    private String fileKey;
    
    @Column(name = "file_url", nullable = false, columnDefinition = "TEXT")
    private String fileUrl;
    
    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;
    
    @Column(name = "bucket_name", nullable = false)
    private String bucketName;
    
    @Column(name = "uploaded_by", nullable = false)
    private Long uploadedBy;
    
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
    
    @Column(name = "deleted")
    private Boolean deleted = false;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
}
```

### 5.2 AttachmentResponse DTO

```java
package com.synergyhub.dto;

import lombok.Data;
import lombok.Builder;

import java.time.LocalDateTime;

@Data
@Builder
public class AttachmentResponse {
    private Long id;
    private Long taskId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String fileUrl;
    private String thumbnailUrl;
    private UserSummary uploadedBy;
    private LocalDateTime uploadedAt;
    
    @Data
    @Builder
    public static class UserSummary {
        private Long id;
        private String name;
    }
}
```

## 6. Service Layer

### 6.1 FileStorageService

```java
package com.synergyhub.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.synergyhub.config.MinioProperties;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileStorageService {
    
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final Tika tika = new Tika();
    
    /**
     * Upload file to MinIO
     */
    public String uploadFile(MultipartFile file, String bucketName, String prefix) 
            throws IOException {
        
        validateFile(file);
        
        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(originalFilename);
        String fileKey = generateFileKey(prefix, extension);
        
        try (InputStream inputStream = file.getInputStream()) {
            // Detect content type
            String contentType = detectContentType(file);
            
            // Upload to MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(fileKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );
            
            log.info("File uploaded successfully: {}", fileKey);
            return fileKey;
            
        } catch (Exception e) {
            log.error("Failed to upload file", e);
            throw new IOException("Failed to upload file: " + e.getMessage());
        }
    }
    
    /**
     * Generate thumbnail for images
     */
    public String uploadThumbnail(MultipartFile file, String bucketName, String fileKey) 
            throws IOException {
        
        if (!isImage(file.getContentType())) {
            return null;
        }
        
        try (InputStream inputStream = file.getInputStream()) {
            BufferedImage originalImage = ImageIO.read(inputStream);
            if (originalImage == null) {
                return null;
            }
            
            // Generate thumbnail (200x150)
            BufferedImage thumbnail = Thumbnails.of(originalImage)
                .size(200, 150)
                .asBufferedImage();
            
            // Upload thumbnail
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(thumbnail, "jpg", outputStream);
            byte[] thumbnailBytes = outputStream.toByteArray();
            
            String thumbnailKey = "thumbnails/" + fileKey;
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(thumbnailKey)
                    .stream(new ByteArrayInputStream(thumbnailBytes), 
                           thumbnailBytes.length, -1)
                    .contentType("image/jpeg")
                    .build()
            );
            
            return thumbnailKey;
            
        } catch (Exception e) {
            log.warn("Failed to generate thumbnail", e);
            return null;
        }
    }
    
    /**
     * Get presigned URL for downloading
     */
    public String getPresignedUrl(String bucketName, String fileKey, int expiryMinutes) 
            throws Exception {
        return minioClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(Method.GET)
                .bucket(bucketName)
                .object(fileKey)
                .expiry(expiryMinutes, TimeUnit.MINUTES)
                .build()
        );
    }
    
    /**
     * Delete file from MinIO
     */
    public void deleteFile(String bucketName, String fileKey) throws Exception {
        minioClient.removeObject(
            RemoveObjectArgs.builder()
                .bucket(bucketName)
                .object(fileKey)
                .build()
        );
        log.info("File deleted: {}", fileKey);
    }
    
    /**
     * Generate unique file key
     */
    private String generateFileKey(String prefix, String extension) {
        String timestamp = LocalDateTime.now()
            .format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String uuid = UUID.randomUUID().toString();
        return String.format("%s/%s/%s.%s", prefix, timestamp, uuid, extension);
    }
    
    /**
     * Validate file
     */
    private void validateFile(MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IOException("File is empty");
        }
        
        String extension = getFileExtension(file.getOriginalFilename());
        if (!minioProperties.getAllowedExtensions().contains(extension.toLowerCase())) {
            throw new IOException("File type not allowed: " + extension);
        }
        
        long maxSize = parseSize(minioProperties.getMaxFileSize());
        if (file.getSize() > maxSize) {
            throw new IOException("File size exceeds limit");
        }
    }
    
    /**
     * Detect content type
     */
    private String detectContentType(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            return tika.detect(inputStream, file.getOriginalFilename());
        }
    }
    
    private String getFileExtension(String filename) {
        return filename.substring(filename.lastIndexOf(".") + 1);
    }
    
    private boolean isImage(String contentType) {
        return contentType != null && contentType.startsWith("image/");
    }
    
    private long parseSize(String size) {
        // Parse "10MB" to bytes
        String number = size.replaceAll("[^0-9]", "");
        long multiplier = size.toUpperCase().contains("GB") ? 1073741824L :
                         size.toUpperCase().contains("MB") ? 1048576L : 1024L;
        return Long.parseLong(number) * multiplier;
    }
}
```

### 6.2 AttachmentService

```java
package com.synergyhub.service;

import com.synergyhub.dto.AttachmentResponse;
import com.synergyhub.entity.Attachment;
import com.synergyhub.repository.AttachmentRepository;
import com.synergyhub.config.MinioProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AttachmentService {
    
    private final AttachmentRepository attachmentRepository;
    private final FileStorageService fileStorageService;
    private final MinioProperties minioProperties;
    private final UserService userService;
    
    /**
     * Upload attachment for a task
     */
    @Transactional
    public AttachmentResponse uploadAttachment(
            Long taskId, 
            MultipartFile file, 
            Long userId
    ) throws Exception {
        
        String bucketName = minioProperties.getBucket().getAttachments();
        String prefix = "tasks/" + taskId;
        
        // Upload file to MinIO
        String fileKey = fileStorageService.uploadFile(file, bucketName, prefix);
        String fileUrl = buildFileUrl(bucketName, fileKey);
        
        // Generate thumbnail for images
        String thumbnailKey = fileStorageService.uploadThumbnail(file, bucketName, fileKey);
        String thumbnailUrl = thumbnailKey != null ? buildFileUrl(bucketName, thumbnailKey) : null;
        
        // Save metadata to database
        Attachment attachment = new Attachment();
        attachment.setTaskId(taskId);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileSize(file.getSize());
        attachment.setFileType(file.getContentType());
        attachment.setFileKey(fileKey);
        attachment.setFileUrl(fileUrl);
        attachment.setThumbnailUrl(thumbnailUrl);
        attachment.setBucketName(bucketName);
        attachment.setUploadedBy(userId);
        attachment.setUploadedAt(LocalDateTime.now());
        
        attachment = attachmentRepository.save(attachment);
        
        log.info("Attachment uploaded for task {}: {}", taskId, file.getOriginalFilename());
        
        return mapToResponse(attachment);
    }
    
    /**
     * Get all attachments for a task
     */
    public List<AttachmentResponse> getTaskAttachments(Long taskId) {
        List<Attachment> attachments = attachmentRepository
            .findByTaskIdAndDeletedFalse(taskId);
        
        return attachments.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
    }
    
    /**
     * Delete attachment (soft delete)
     */
    @Transactional
    public void deleteAttachment(Long attachmentId, Long userId) throws Exception {
        Attachment attachment = attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new RuntimeException("Attachment not found"));
        
        // Soft delete in database
        attachment.setDeleted(true);
        attachment.setDeletedAt(LocalDateTime.now());
        attachmentRepository.save(attachment);
        
        // Delete from MinIO (optional - could be done by background job)
        try {
            fileStorageService.deleteFile(attachment.getBucketName(), attachment.getFileKey());
            if (attachment.getThumbnailUrl() != null) {
                String thumbnailKey = "thumbnails/" + attachment.getFileKey();
                fileStorageService.deleteFile(attachment.getBucketName(), thumbnailKey);
            }
        } catch (Exception e) {
            log.error("Failed to delete file from MinIO", e);
        }
        
        log.info("Attachment deleted: {}", attachmentId);
    }
    
    /**
     * Get download URL (presigned URL)
     */
    public String getDownloadUrl(Long attachmentId) throws Exception {
        Attachment attachment = attachmentRepository.findById(attachmentId)
            .orElseThrow(() -> new RuntimeException("Attachment not found"));
        
        // Generate presigned URL valid for 15 minutes
        return fileStorageService.getPresignedUrl(
            attachment.getBucketName(), 
            attachment.getFileKey(), 
            15
        );
    }
    
    private String buildFileUrl(String bucketName, String fileKey) {
        return String.format("%s/%s/%s", 
            minioProperties.getPublicUrl(), bucketName, fileKey);
    }
    
    private AttachmentResponse mapToResponse(Attachment attachment) {
        var user = userService.findById(attachment.getUploadedBy());
        
        return AttachmentResponse.builder()
            .id(attachment.getId())
            .taskId(attachment.getTaskId())
            .fileName(attachment.getFileName())
            .fileSize(attachment.getFileSize())
            .fileType(attachment.getFileType())
            .fileUrl(attachment.getFileUrl())
            .thumbnailUrl(attachment.getThumbnailUrl())
            .uploadedBy(AttachmentResponse.UserSummary.builder()
                .id(user.getId())
                .name(user.getName())
                .build())
            .uploadedAt(attachment.getUploadedAt())
            .build();
    }
}
```

## 7. Repository Layer

```java
package com.synergyhub.repository;

import com.synergyhub.entity.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {
    
    List<Attachment> findByTaskIdAndDeletedFalse(Long taskId);
    
    List<Attachment> findByUploadedByAndDeletedFalse(Long userId);
    
    long countByTaskIdAndDeletedFalse(Long taskId);
    
    boolean existsByIdAndTaskId(Long id, Long taskId);
}
```

## 8. Controller Layer

```java
package com.synergyhub.controller;

import com.synergyhub.dto.ApiResponse;
import com.synergyhub.dto.AttachmentResponse;
import com.synergyhub.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/attachments")
@RequiredArgsConstructor
public class AttachmentController {
    
    private final AttachmentService attachmentService;
    
    /**
     * POST /api/attachments/upload
     * Upload file attachment for a task
     */
    @PostMapping("/upload")
    public ResponseEntity<ApiResponse<AttachmentResponse>> uploadAttachment(
            @RequestParam("file") MultipartFile file,
            @RequestParam("taskId") Long taskId,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            Long userId = ((CustomUserDetails) userDetails).getId();
            AttachmentResponse attachment = attachmentService.uploadAttachment(
                taskId, file, userId
            );
            
            return ResponseEntity.ok(
                ApiResponse.success("File uploaded successfully", attachment)
            );
            
        } catch (Exception e) {
            log.error("Failed to upload attachment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to upload file: " + e.getMessage()));
        }
    }
    
    /**
     * GET /api/attachments/task/{taskId}
     * Get all attachments for a task
     */
    @GetMapping("/task/{taskId}")
    public ResponseEntity<ApiResponse<List<AttachmentResponse>>> getTaskAttachments(
            @PathVariable Long taskId
    ) {
        List<AttachmentResponse> attachments = attachmentService.getTaskAttachments(taskId);
        return ResponseEntity.ok(
            ApiResponse.success("Attachments retrieved", attachments)
        );
    }
    
    /**
     * GET /api/attachments/{id}/download-url
     * Get presigned download URL
     */
    @GetMapping("/{id}/download-url")
    public ResponseEntity<ApiResponse<String>> getDownloadUrl(
            @PathVariable Long id
    ) {
        try {
            String url = attachmentService.getDownloadUrl(id);
            return ResponseEntity.ok(
                ApiResponse.success("Download URL generated", url)
            );
        } catch (Exception e) {
            log.error("Failed to generate download URL", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to generate download URL"));
        }
    }
    
    /**
     * DELETE /api/attachments/{id}
     * Delete attachment
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAttachment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        try {
            Long userId = ((CustomUserDetails) userDetails).getId();
            attachmentService.deleteAttachment(id, userId);
            
            return ResponseEntity.ok(
                ApiResponse.success("Attachment deleted", null)
            );
            
        } catch (Exception e) {
            log.error("Failed to delete attachment", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Failed to delete attachment"));
        }
    }
}
```

## 9. Security Considerations

### 9.1 File Validation

- Validate file extensions
- Validate MIME types using Apache Tika
- Check file size limits
- Scan for malicious content (optional - ClamAV)

### 9.2 Access Control

```java
@Service
public class AttachmentSecurityService {
    
    public boolean canUploadToTask(Long userId, Long taskId) {
        // Check if user is member of task's project
        // Check user role permissions
        return true; // Implement logic
    }
    
    public boolean canDeleteAttachment(Long userId, Long attachmentId) {
        // Check if user uploaded the file OR is admin
        return true; // Implement logic
    }
    
    public boolean canViewAttachment(Long userId, Long attachmentId) {
        // Check if user has access to the task
        return true; // Implement logic
    }
}
```

### 9.3 MinIO Security

```bash
# Enable HTTPS
mc admin config set myminio api secure=on

# Set CORS policy
mc admin config set myminio api cors_allow_origin="https://yourdomain.com"

# Enable encryption at rest (optional)
mc encrypt set sse-s3 myminio/synergyhub-attachments
```

## 10. Frontend Integration

### 10.1 Update Attachment Service

```typescript
// services/attachment.service.ts
import axios from "axios";
import type { Attachment, AttachmentResponse } from "@/types/attachment.types";

const API_BASE = "/api/attachments";

export const attachmentService = {
  async uploadAttachment(taskId: number, file: File): Promise<Attachment> {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("taskId", taskId.toString());

    const response = await axios.post<AttachmentResponse>(
      `${API_BASE}/upload`,
      formData,
      {
        headers: {
          "Content-Type": "multipart/form-data",
        },
      }
    );

    return response.data.data;
  },

  async getTaskAttachments(taskId: number): Promise<Attachment[]> {
    const response = await axios.get<AttachmentResponse>(
      `${API_BASE}/task/${taskId}`
    );
    return response.data.data;
  },

  async deleteAttachment(attachmentId: number): Promise<void> {
    await axios.delete(`${API_BASE}/${attachmentId}`);
  },

  async getDownloadUrl(attachmentId: number): Promise<string> {
    const response = await axios.get<AttachmentResponse>(
      `${API_BASE}/${attachmentId}/download-url`
    );
    return response.data.data;
  },
};
```

### 10.2 Replace Mock Implementation

Remove `mockAttachments.ts` imports and use real API:

```typescript
// In IssueAttachmentsSection.tsx
import { attachmentService } from "@/services/attachment.service";
import { useQuery, useMutation, useQueryClient } from "@tanstack/react-query";

export function IssueAttachmentsSection({ taskId, isReadOnly }: Props) {
  const queryClient = useQueryClient();

  // Fetch attachments
  const { data: attachments = [] } = useQuery({
    queryKey: ["attachments", taskId],
    queryFn: () => attachmentService.getTaskAttachments(taskId),
  });

  // Upload mutation
  const uploadMutation = useMutation({
    mutationFn: (file: File) => attachmentService.uploadAttachment(taskId, file),
    onSuccess: () => {
      queryClient.invalidateQueries(["attachments", taskId]);
      toast.success("File uploaded successfully");
    },
  });

  // Delete mutation
  const deleteMutation = useMutation({
    mutationFn: (id: number) => attachmentService.deleteAttachment(id),
    onSuccess: () => {
      queryClient.invalidateQueries(["attachments", taskId]);
      toast.success("Attachment deleted");
    },
  });

  // ... rest of component
}
```

## 11. Performance Optimization

### 11.1 Lazy Loading

- Load attachment thumbnails on demand
- Use pagination for tasks with many attachments
- Implement virtual scrolling for large lists

### 11.2 Caching

```java
@Cacheable(value = "attachments", key = "#taskId")
public List<AttachmentResponse> getTaskAttachments(Long taskId) {
    // ...
}

@CacheEvict(value = "attachments", key = "#taskId")
public void uploadAttachment(Long taskId, /*...*/) {
    // ...
}
```

### 11.3 CDN (Optional)

For public files, consider using Cloudflare or similar CDN in front of MinIO.

## 12. Backup Strategy

### 12.1 MinIO Data Backup

```bash
# Backup MinIO data directory
tar -czf minio-backup-$(date +%Y%m%d).tar.gz /opt/minio/data

# Or use MinIO replication to backup server
mc admin replicate add myminio backup-minio
```

### 12.2 Database Backup

```bash
# Backup attachment metadata
pg_dump -t attachment synergyhub_db > attachments_backup.sql
```

## 13. Monitoring & Logging

### 13.1 MinIO Monitoring

- Enable Prometheus metrics: `http://localhost:9000/minio/prometheus/metrics`
- Set up Grafana dashboard for MinIO
- Configure alerts for storage capacity

### 13.2 Application Logging

```java
@Aspect
@Component
@Slf4j
public class AttachmentLoggingAspect {
    
    @AfterReturning("execution(* com.synergyhub.service.AttachmentService.upload*(..))")
    public void logUpload(JoinPoint joinPoint) {
        log.info("File uploaded: {}", joinPoint.getArgs());
    }
    
    @AfterReturning("execution(* com.synergyhub.service.AttachmentService.delete*(..))")
    public void logDeletion(JoinPoint joinPoint) {
        log.info("File deleted: {}", joinPoint.getArgs());
    }
}
```

## 14. Testing

### 14.1 Unit Tests

```java
@Test
void shouldUploadFile() {
    MockMultipartFile file = new MockMultipartFile(
        "file",
        "test.pdf",
        "application/pdf",
        "test content".getBytes()
    );
    
    AttachmentResponse response = attachmentService.uploadAttachment(
        1L, file, 1L
    );
    
    assertNotNull(response.getId());
    assertEquals("test.pdf", response.getFileName());
}
```

### 14.2 Integration Tests

Test with actual MinIO (using Testcontainers):

```java
@Testcontainers
class AttachmentServiceIntegrationTest {
    
    @Container
    static MinIOContainer minioContainer = new MinIOContainer("minio/minio:latest")
        .withUserName("minioadmin")
        .withPassword("minioadmin");
    
    // ... tests
}
```

## 15. Deployment Checklist

- [ ] Install MinIO on VPS
- [ ] Configure MinIO buckets and policies
- [ ] Set up Nginx reverse proxy
- [ ] Configure SSL/TLS certificates
- [ ] Update backend configuration
- [ ] Run database migrations
- [ ] Deploy backend changes
- [ ] Update frontend to use real API
- [ ] Test file upload/download
- [ ] Set up monitoring and alerts
- [ ] Configure backup jobs
- [ ] Document API endpoints

## 16. Rollback Plan

If issues occur:

1. Revert frontend to use mock data
2. Revert backend deployment
3. Keep MinIO running (no data loss)
4. Investigate issues in staging
5. Fix and redeploy

## 17. Cost Estimation

**On-Premise VPS Costs (monthly):**
- Storage: ~$0.02/GB (varies by provider)
- For 100GB storage: ~$2/month
- Bandwidth: Usually included in VPS plan
- No S3 API fees (unlike AWS)

**Comparison with AWS S3:**
- AWS S3: ~$0.023/GB + API fees
- For 100GB: ~$2.30/month + API fees
- On-premise is cost-effective for high usage

## 18. Future Enhancements

- [ ] Virus scanning integration (ClamAV)
- [ ] Image optimization/compression
- [ ] Video thumbnail generation
- [ ] File versioning
- [ ] Bulk upload support
- [ ] Direct browser-to-S3 upload (presigned POST)
- [ ] Content delivery network (CDN) integration
- [ ] File sharing with expiration links

---

**Next Steps:**
1. Set up MinIO on VPS
2. Implement backend services
3. Run database migrations
4. Update frontend to use real API
5. Test thoroughly in staging environment
6. Deploy to production
