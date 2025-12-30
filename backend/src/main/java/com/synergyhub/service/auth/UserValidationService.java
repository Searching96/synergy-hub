package com.synergyhub.service.auth;

import com.synergyhub.exception.BadRequestException;
import com.synergyhub.exception.EmailAlreadyExistsException;
import com.synergyhub.repository.UserRepository;
import com.synergyhub.util.PasswordValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserValidationService {
    private final UserRepository userRepository;
    private final PasswordValidator passwordValidator;
    private final PasswordEncoder passwordEncoder;

    public void validateEmailUniqueness(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
    }

    public void validatePassword(String password) {
        if (!passwordValidator.isValid(password)) {
            throw new BadRequestException("Password does not meet requirements: " + passwordValidator.getRequirements());
        }
    }

    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }
}
