package com.synergyhub.auth.security;

import com.synergyhub.auth.entity.Role;
import com.synergyhub.auth.entity.User;
import com.synergyhub.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        Role role = Role.builder()
            .roleId(1)
            .name("Team Member")
            .build();

        testUser = User.builder()
            .userId(1)
            .name("Test User")
            .email("test@example.com")
            .passwordHash("hashedPassword")
            .organizationId(1)
            .twoFactorEnabled(false)
            .accountLocked(false)
            .emailVerified(true)
            .failedLoginAttempts(0)
            .roles(Set.of(role))
            .build();
    }

    @Test
    void loadUserByUsername_WithExistingUser_ShouldReturnUserDetails() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Assert
        assertNotNull(userDetails);
        assertEquals("test@example.com", userDetails.getUsername());
        assertEquals("hashedPassword", userDetails.getPassword());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.isEnabled());
    }

    @Test
    void loadUserByUsername_WithNonExistingUser_ShouldThrowException() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () ->
            userDetailsService.loadUserByUsername("nonexistent@example.com"));
    }

    @Test
    void loadUserByUsername_ShouldLoadAuthorities() {
        // Arrange
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Assert
        assertFalse(userDetails.getAuthorities().isEmpty());
        assertTrue(userDetails.getAuthorities().stream()
            .anyMatch(auth -> auth.getAuthority().equals("ROLE_TEAM_MEMBER")));
    }
}