package com.synergyhub.config;

import com.synergyhub.security.UserContextArgumentResolver;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    private final UserContextArgumentResolver userContextArgumentResolver;

    public WebConfig(UserContextArgumentResolver userContextArgumentResolver) {
        this.userContextArgumentResolver = userContextArgumentResolver;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(userContextArgumentResolver);
    }
}
