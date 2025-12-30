package com.synergyhub.service.auth;

import java.util.List;

public class MockBackupCodeService implements BackupCodeService {
    private List<String> codes;
    @Override
    public List<String> generateBackupCodes(int count, int length) {
        return codes;
    }
    @Override
    public boolean verifyBackupCode(String userId, String code) {
        return codes != null && codes.contains(code);
    }
    @Override
    public void consumeBackupCode(String userId, String code) {
        if (codes != null) codes.remove(code);
    }
    @Override
    public List<String> getBackupCodes(String userId) {
        return codes;
    }
    @Override
    public void setBackupCodes(String userId, List<String> codes) {
        this.codes = codes;
    }
}
