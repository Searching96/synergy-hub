package com.synergyhub.config;

import com.synergyhub.security.CustomUserDetailsService;
import com.synergyhub.security.JwtAuthenticationEntryPoint;
import com.synergyhub.security.JwtAuthenticationFilter;
import com.synergyhub.security.JwtTokenProvider;
import com.synergyhub.service.auth.SessionService;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@Slf4j
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final SessionService sessionService;
    private final CorsConfigurationSource corsConfigurationSource;
    
    // OAuth2 dependencies - optional (may be null if OAuth2 is not configured)
    private final com.synergyhub.security.oauth2.CustomOAuth2UserService customOAuth2UserService;
    private final com.synergyhub.security.oauth2.OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final com.synergyhub.security.oauth2.OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final ClientRegistrationRepository clientRegistrationRepository;

    @Autowired
    public SecurityConfig(
            CustomUserDetailsService customUserDetailsService,
            JwtTokenProvider jwtTokenProvider,
            SessionService sessionService,
            CorsConfigurationSource corsConfigurationSource,
            @Autowired(required = false) com.synergyhub.security.oauth2.CustomOAuth2UserService customOAuth2UserService,
            @Autowired(required = false) com.synergyhub.security.oauth2.OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
            @Autowired(required = false) com.synergyhub.security.oauth2.OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler,
            @Autowired(required = false) ClientRegistrationRepository clientRegistrationRepository) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.sessionService = sessionService;
        this.corsConfigurationSource = corsConfigurationSource;
        this.customOAuth2UserService = customOAuth2UserService;
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
        this.clientRegistrationRepository = clientRegistrationRepository;
    }

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
            .cors(cors -> cors.configurationSource(corsConfigurationSource))
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(new JwtAuthenticationEntryPoint()) 
            )
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/api/auth/**",
                    "/api/public/**",
                    "/api/oauth2/**",
                    "/actuator/health/**",
                    "/swagger-ui/**", 
                    "/v3/api-docs/**"
                ).permitAll()
                .anyRequest().authenticated()
            );

        // Only configure OAuth2 if client registrations are available
        if (clientRegistrationRepository != null && customOAuth2UserService != null) {
            log.info("OAuth2 login is enabled - client registrations found");
            http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(auth -> auth
                    .baseUri("/api/oauth2/authorize")
                )
                .redirectionEndpoint(redir -> redir
                    .baseUri("/api/oauth2/callback/*")
                )
                .userInfoEndpoint(userInfo -> userInfo
                    .userService(customOAuth2UserService)
                )
                .successHandler(oAuth2AuthenticationSuccessHandler)
                .failureHandler(oAuth2AuthenticationFailureHandler)
            );
        } else {
            log.warn("OAuth2 login is disabled - no client registrations configured. Set GOOGLE_CLIENT_ID/GITHUB_CLIENT_ID to enable.");
        }

        http.headers(headers -> headers
            .frameOptions(frame -> frame.deny())
            .xssProtection(xss -> xss.disable()) 
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
