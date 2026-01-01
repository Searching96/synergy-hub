package com.synergyhub.service.auth;

import com.synergyhub.domain.entity.BackupCode;
import com.synergyhub.domain.entity.User;
import com.synergyhub.exception.ResourceNotFoundException;
import com.synergyhub.repository.BackupCodeRepository;
import com.synergyhub.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder; // ✅ Import added
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
    private final PasswordEncoder passwordEncoder; // ✅ Inject BCrypt/Argon2 encoder

    @Override
    @Transactional
    public List<String> generateBackupCodes(int count, int length) {
        // Generates random plaintext codes. 
        // These should be returned to the Controller to be shown to the user ONCE.
        List<String> codes = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            codes.add(generateRandomCode(length));
        }
        return codes;
    }

    @Override
    @Transactional
    public void setBackupCodes(String userId, List<String> codes) {
        User user = getUser(userId);

        // 1. Invalidate/Delete existing codes
        backupCodeRepository.deleteByUser(user);

        // 2. Save new codes (HASHED)
        List<BackupCode> entities = codes.stream()
                .map(code -> BackupCode.builder()
                        .user(user)
                        .code(passwordEncoder.encode(code)) // ✅ HASH IT (Secure Storage)
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
        
        // We cannot query by specific code because we only have hashes.
        // We must fetch all unused codes and match them in memory.
        List<BackupCode> codes = backupCodeRepository.findByUserAndUsedFalse(user);
        
        return codes.stream()
                .anyMatch(bc -> passwordEncoder.matches(code, bc.getCode())); // ✅ Verify Hash
    }

    @Override
    @Transactional
    public void consumeBackupCode(String userId, String code) {
        User user = getUser(userId);
        
        // Fetch all unused codes for the user
        List<BackupCode> codes = backupCodeRepository.findByUserAndUsedFalse(user);

        // Find the specific code that matches the input
        codes.stream()
                .filter(bc -> passwordEncoder.matches(code, bc.getCode()))
                .findFirst()
                .ifPresent(backupCode -> {
                    backupCode.setUsed(true);
                    backupCodeRepository.save(backupCode);
                    log.info("Backup code consumed for user {}", userId);
                });
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getBackupCodes(String userId) {
        // ⚠️ SECURITY NOTE: 
        // Since we now hash the codes, we CANNOT retrieve the plaintext versions.
        // You should remove the "View Backup Codes" feature from your UI, 
        // as codes can only be viewed at the moment of generation.
        throw new UnsupportedOperationException("Cannot retrieve plaintext backup codes after they have been hashed and saved.");
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