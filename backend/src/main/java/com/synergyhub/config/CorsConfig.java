package com.synergyhub.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Configuration
@Slf4j
public class CorsConfig {

    @Value("${app.cors.allowed-origins}")
    private String[] allowedOrigins;

    @PostConstruct
    public void validateCorsConfig() {
        // ✅ Validate that wildcard is not used with credentials
        List<String> origins = Arrays.asList(allowedOrigins);
        
        if (origins.contains("*")) {
            throw new IllegalStateException(
                "CORS configuration error: Cannot use wildcard '*' with allowCredentials=true. " +
                "Please specify explicit origins in app.cors.allowed-origins"
            );
        }
        
        // ✅ Validate origin format
        origins.forEach(origin -> {
            if (!origin.startsWith("http://") && !origin.startsWith("https://")) {
                throw new IllegalStateException(
                    "CORS configuration error: Invalid origin format '" + origin + "'. " +
                    "Origins must start with http:// or https://"
                );
            }
        });
        
        log.info("✅ CORS configuration validated successfully. Allowed origins: {}", 
                 String.join(", ", origins));
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // ✅ Validated origins
        configuration.setAllowedOrigins(Arrays.asList(allowedOrigins));
        
        // ✅ Explicit methods only
        configuration.setAllowedMethods(Arrays.asList(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
        ));
        
        // ✅ Explicit headers (not "*")
        configuration.setAllowedHeaders(Arrays.asList(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
        ));
        
        // ✅ Expose only necessary headers
        configuration.setExposedHeaders(Arrays.asList(
            "Authorization",
            "Content-Disposition"
        ));
        
        // ✅ Credentials allowed (validated to not use "*")
        configuration.setAllowCredentials(true);
        
        // ✅ Cache preflight for 1 hour
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}