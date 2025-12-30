package com.synergyhub.service.auth;

public class MockTotpService implements TotpService {
    public String secret;
    public String qrCodeUrl;
    public boolean validCode = true;
    @Override
    public String generateSecret() { return secret; }
    @Override
    public String generateQrCodeUrl(String secret, String email) { return qrCodeUrl; }
    @Override
    public boolean verifyCode(String secret, String code) { return validCode; }
}
