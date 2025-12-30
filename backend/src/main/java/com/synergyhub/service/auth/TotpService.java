package com.synergyhub.service.auth;

public interface TotpService {
    String generateSecret();
    String generateQrCodeUrl(String secret, String email);
    boolean verifyCode(String secret, String code);
}
