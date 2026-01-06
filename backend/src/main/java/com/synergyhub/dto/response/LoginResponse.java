package com.synergyhub.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    
    @Builder.Default
    private String tokenTypeValue = "Bearer";
    
    private Long expiresIn; // in seconds
    private UserResponse user;
    private boolean twoFactorRequired;
    private String twoFactorToken; // Temporary token for 2FA completion
}