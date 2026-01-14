package com.synergyhub.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttachmentResponse {
    private Long id;
    private Long taskId;
    private String fileName;
    private Long fileSize;
    private String fileType;
    private String fileUrl;
    private String thumbnailUrl;
    private String bucketName;
    private Long uploadedBy;
    private String uploaderName;
    private LocalDateTime uploadedAt;

    public String getFileSizeFormatted() {
        if (fileSize == null) return "0 B";
        
        long bytes = fileSize;
        if (bytes <= 0) return "0 B";
        
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        
        return String.format("%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    public String getFileExtension() {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1).toLowerCase() : "";
    }

    public boolean isImage() {
        String ext = getFileExtension();
        return ext.matches("jpg|jpeg|png|gif|webp|svg");
    }

    public boolean isPdf() {
        return getFileExtension().equals("pdf");
    }
}
