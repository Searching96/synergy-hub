package com.synergyhub.service.auth;

import java.util.List;

public interface BackupCodeService {
    List<String> generateBackupCodes(int count, int length);
    boolean verifyBackupCode(String userId, String code);
    void consumeBackupCode(String userId, String code);
    List<String> getBackupCodes(String userId);
    void setBackupCodes(String userId, List<String> codes);
}
