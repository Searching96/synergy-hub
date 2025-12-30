package com.synergyhub.service.auth;

import com.synergyhub.util.TotpUtil;
import org.springframework.stereotype.Service;

@Service
public class DefaultTotpService implements TotpService {
    private final TotpUtil totpUtil;

    public DefaultTotpService(TotpUtil totpUtil) {
        this.totpUtil = totpUtil;
    }

    @Override
    public String generateSecret() {
        return totpUtil.generateSecret();
    }

    @Override
    public String generateQrCodeUrl(String secret, String email) {
        return totpUtil.generateQrCodeUrl(secret, email);
    }

    @Override
    public boolean verifyCode(String secret, String code) {
        return totpUtil.verifyCode(secret, code);
    }
}
