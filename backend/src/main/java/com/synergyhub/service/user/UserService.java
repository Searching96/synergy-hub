package com.synergyhub.service.user;

import com.synergyhub.domain.entity.User;
import com.synergyhub.dto.mapper.UserMapper;
import com.synergyhub.dto.request.UpdateProfileRequest;
import com.synergyhub.dto.response.UserResponse;
import com.synergyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.synergyhub.config.MinioProperties;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final MinioClient minioClient;
    private final MinioProperties minioProperties;

    @Transactional
    public UserResponse updateProfile(User user, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", user.getEmail());
        
        user.setName(request.getName());
        // Add other fields here if needed in the future
        
        User savedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
        
        return userMapper.toUserResponse(savedUser);
    }

    @Transactional
    public UserResponse uploadAvatar(User user, MultipartFile file) {
        log.info("Uploading avatar for user: {}", user.getEmail());

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!minioProperties.getAllowedExtensions().contains(extension.toLowerCase())) {
             throw new IllegalArgumentException("File type not allowed");
        }

        String bucket = minioProperties.getBucket().getAvatars();
        String fileKey = "avatar_" + user.getId() + "_" + UUID.randomUUID() + "." + extension;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(fileKey)
                            .stream(inputStream, file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

            // Generate URL
            String avatarUrl = minioProperties.getPublicUrl().replaceAll("/$", "") + "/" + bucket + "/" + fileKey;
            
            user.setImageUrl(avatarUrl);
            User savedUser = userRepository.save(user);
            
            log.info("Avatar uploaded for user: {}", user.getEmail());
            return userMapper.toUserResponse(savedUser);

        } catch (Exception e) {
            log.error("Failed to upload avatar", e);
            throw new RuntimeException("Failed to upload avatar", e);
        }
    }

    private String getFileExtension(String fileName) {
        if (fileName == null) return "";
        int lastDot = fileName.lastIndexOf('.');
        return lastDot > 0 ? fileName.substring(lastDot + 1) : "";
    }
}
