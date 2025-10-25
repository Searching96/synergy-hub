package com.synergyhub.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.synergyhub.auth.AllTestsSuite;
import com.synergyhub.auth.dto.LoginRequest;
import com.synergyhub.auth.dto.RegisterRequest;
import com.synergyhub.auth.entity.Role;
import com.synergyhub.auth.entity.User;
import com.synergyhub.auth.repository.RoleRepository;
import com.synergyhub.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = AllTestsSuite.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Disabled("Integration tests disabled - requires full Spring context setup with database")
class AuthenticationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Role guestRole;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        guestRole = Role.builder()
                .name("Guest")
                .description("Guest role")
                .permissions(new HashSet<>())
                .build();
        roleRepository.save(guestRole);
    }

    @Test
    void fullAuthenticationFlow_ShouldWork() throws Exception {
        // 1. Register
        RegisterRequest registerRequest = new RegisterRequest(
                "Integration Test User",
                "integration@example.com",
                "Password123",
                1,
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value("integration@example.com"));

        // Manually verify email for testing
        User user = userRepository.findByEmail("integration@example.com").orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);

        // 2. Login
        LoginRequest loginRequest = new LoginRequest("integration@example.com", "Password123", null);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").exists())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"));
    }

    @Test
    void login_WithWrongPassword_ShouldFail() throws Exception {
        // Create user
        User user = User.builder()
                .name("Test User")
                .email("test@example.com")
                .passwordHash(passwordEncoder.encode("CorrectPassword123"))
                .organizationId(1)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .roles(Set.of(guestRole))
                .build();
        userRepository.save(user);

        // Try login with wrong password
        LoginRequest loginRequest = new LoginRequest("test@example.com", "WrongPassword", null);

        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void register_WithExistingEmail_ShouldFail() throws Exception {
        // Create existing user
        User user = User.builder()
                .name("Existing User")
                .email("existing@example.com")
                .passwordHash(passwordEncoder.encode("Password123"))
                .organizationId(1)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .roles(Set.of(guestRole))
                .build();
        userRepository.save(user);

        // Try to register with same email
        RegisterRequest registerRequest = new RegisterRequest(
                "New User",
                "existing@example.com",
                "Password123",
                1,
                null
        );

        mockMvc.perform(post("/api/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    void login_WithMultipleFailedAttempts_ShouldLockAccount() throws Exception {
        // Create user
        User user = User.builder()
                .name("Test User")
                .email("locktest@example.com")
                .passwordHash(passwordEncoder.encode("CorrectPassword123"))
                .organizationId(1)
                .twoFactorEnabled(false)
                .accountLocked(false)
                .emailVerified(true)
                .failedLoginAttempts(0)
                .roles(Set.of(guestRole))
                .build();
        userRepository.save(user);

        LoginRequest loginRequest = new LoginRequest("locktest@example.com", "WrongPassword", null);

        // Attempt login 5 times with wrong password
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(loginRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt should return account locked
        mockMvc.perform(post("/api/auth/login")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isForbidden());
    }
}