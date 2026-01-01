package com.synergyhub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * Configuration properties for pagination across the application.
 * These values can be externalized to application.yml for environment-specific tuning.
 */
@Configuration
@ConfigurationProperties(prefix = "app.pagination")
@Validated
@Getter
@Setter
public class PaginationConfig {
    
    /**
     * Default page size for paginated queries
     */
    @Min(1)
    @Max(100)
    private int defaultPageSize = 20;
    
    /**
     * Maximum allowed page size to prevent DoS attacks
     */
    @Min(1)
    @Max(1000)
    private int maxPageSize = 100;
    
    /**
     * Default page number (0-indexed)
     */
    @Min(0)
    private int defaultPage = 0;
}
