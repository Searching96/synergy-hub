package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.BackupCode;
import com.synergyhub.domain.entity.User;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.BackupCodeRepository;
import com.synergyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultBackupCodeService implements BackupCodeService {

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final SecureRandom random = new SecureRandom();
    
    private final BackupCodeRepository backupCodeRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public List<String> generateBackupCodes(int count, int length) {
        // This method signature in your interface is slightly flawed because it 
        // doesn't take a userId, but your implementation implies it creates them generic?
        // Usually, you generate codes FOR a user.
        // Assuming the Controller handles the User context and calls setBackupCodes, 
        // or this method is just a helper utility.
        
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            codes.add(generateRandomCode(length));
        }
        return codes;
    }

    /**
     * Generates and Saves codes for a specific user (Recommended Method)
     * You might need to add this to your Interface or use setBackupCodes logic.
     */
    @Override
    @Transactional
    public void setBackupCodes(String userId, List<String> codes) {
        User user = getUser(userId);

        // 1. Invalidate/Delete existing codes
        backupCodeRepository.deleteByUser(user);

        // 2. Save new codes
        List<BackupCode> entities = codes.stream()
                .map(code -> BackupCode.builder()
                        .user(user)
                        .code(code) // In high security apps, hash this!
                        .used(false)
                        .build())
                .collect(Collectors.toList());

        backupCodeRepository.saveAll(entities);
        log.info("Generated {} new backup codes for user {}", codes.size(), userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean verifyBackupCode(String userId, String code) {
        User user = getUser(userId);
        return backupCodeRepository.findByUserAndCodeAndUsedFalse(user, code).isPresent();
    }

    @Override
    @Transactional
    public void consumeBackupCode(String userId, String code) {
        User user = getUser(userId);
        backupCodeRepository.findByUserAndCodeAndUsedFalse(user, code)
                .ifPresent(backupCode -> {
                    backupCode.setUsed(true);
                    backupCodeRepository.save(backupCode);
                    log.info("Backup code consumed for user {}", userId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getBackupCodes(String userId) {
        User user = getUser(userId);
        return backupCodeRepository.findByUserAndUsedFalse(user).stream()
                .map(BackupCode::getCode)
                .collect(Collectors.toList());
    }

    private User getUser(String userId) {
        return userRepository.findById(Integer.parseInt(userId))
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));
    }

    private String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}