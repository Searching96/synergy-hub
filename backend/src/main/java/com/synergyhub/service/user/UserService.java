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

@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;

    @Transactional
    public UserResponse updateProfile(User user, UpdateProfileRequest request) {
        log.info("Updating profile for user: {}", user.getEmail());
        
        user.setName(request.getName());
        // Add other fields here if needed in the future
        
        User savedUser = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
        
        return userMapper.toUserResponse(savedUser);
    }
}
