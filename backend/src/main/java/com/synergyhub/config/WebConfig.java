package com.synergyhub.config;

import com.synergyhub.security.OrganizationContextInterceptor;
import com.synergyhub.security.UserContextArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final UserContextArgumentResolver userContextArgumentResolver;
    private final OrganizationContextInterceptor organizationContextInterceptor;

    public WebConfig(
            UserContextArgumentResolver userContextArgumentResolver,
            OrganizationContextInterceptor organizationContextInterceptor) {
        this.userContextArgumentResolver = userContextArgumentResolver;
        this.organizationContextInterceptor = organizationContextInterceptor;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userContextArgumentResolver);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Add organization context interceptor for all authenticated requests
        // Exclude public endpoints that don't need organization context
        registry.addInterceptor(organizationContextInterceptor)
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/public/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/actuator/health/**"
                );
    }
}
