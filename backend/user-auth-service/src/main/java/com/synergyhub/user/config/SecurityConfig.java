package com.synergyhub.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // Modern syntax for disabling CSRF
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/users/**").authenticated() // Match all /users endpoints
                .anyRequest().permitAll()
            )
            .httpBasic(httpBasic -> {}); // Modern syntax for enabling HTTP Basic Auth
        return http.build();
    }
}
