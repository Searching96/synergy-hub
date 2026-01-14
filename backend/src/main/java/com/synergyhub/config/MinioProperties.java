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
