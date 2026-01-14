package com.synergyhub.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateProfileRequest {
    
    @NotBlank(message = "Name cannot be blank")
    @Size(min = 2, max = 100, message = "Name must be between 2 and 100 characters")
    private String name;
    
    // Check if we allow email updates? Usually requires re-verification. 
    // For now, let's keep it simple as per plan (Profile Updates).
    // If we want to allow email update, specific logic is needed.
}
