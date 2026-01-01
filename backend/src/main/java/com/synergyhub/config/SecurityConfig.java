package com.synergyhub.config;

import com.synergyhub.security.CustomUserDetailsService;
import com.synergyhub.security.JwtAuthenticationEntryPoint;
import com.synergyhub.security.JwtAuthenticationFilter;
import com.synergyhub.security.JwtTokenProvider;
import com.synergyhub.service.auth.SessionService; // Ensure this imports correctly

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor // ✅ Handles all "private final" injections automatically
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final CorsConfigurationSource corsConfigurationSource; // ✅ Must be defined in another config class

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtTokenProvider, sessionService, customUserDetailsService);
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // ✅ Use the injected source directly
            .cors(cors -> cors.configurationSource(corsConfigurationSource)) 
            .exceptionHandling(exception -> exception
                // ✅ Use the injected bean directly (no need for @Bean method call if injected)
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()) 
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                // ✅ PUBLIC AUTH ENDPOINTS
                .requestMatchers(
                    "/api/auth/login",
                    "/api/auth/register",
                    "/api/auth/verify-email",
                    "/api/auth/resend-verification",
                    "/api/auth/forgot-password",
                    "/api/auth/validate-reset-token",
                    "/api/auth/reset-password",
                    "/api/public/**",
                    "/actuator/health/**",
                    "/swagger-ui/**", 
                    "/v3/api-docs/**"
                ).permitAll()
                
                // ✅ ALL OTHER ENDPOINTS
                .anyRequest().authenticated()
            );

        // ✅ SECURITY HEADERS
        http.headers(headers -> headers
            .frameOptions(frame -> frame.deny())
            .xssProtection(xss -> xss.disable()) // OK if using CSP
            .contentSecurityPolicy(csp -> csp
                .policyDirectives("default-src 'self'; frame-ancestors 'none'; form-action 'self'")
            )
            .httpStrictTransportSecurity(hsts -> hsts
                .includeSubDomains(true)
                .maxAgeInSeconds(31536000))
        );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}