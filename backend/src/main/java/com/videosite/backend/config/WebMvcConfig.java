package com.videosite.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final UserAuthInterceptor userAuthInterceptor;

    @Value("${app.cors.allowed-origin:http://localhost:5173}")
    private String corsAllowedOrigin;

    @Value("${app.storage.local-root:../}")
    private String localStorageRoot;

    public WebMvcConfig(AdminAuthInterceptor adminAuthInterceptor,
                        UserAuthInterceptor userAuthInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.userAuthInterceptor = userAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
            .excludePathPatterns(
                "/api/admin/auth/login",
                "/api/admin/auth/me",
                "/api/admin/auth/logout"
            );

        registry.addInterceptor(userAuthInterceptor)
            .addPathPatterns(
                "/api/users/me/**",
                "/api/videos/*/likes"
            );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
            .allowedOriginPatterns(
                corsAllowedOrigin,
                "http://localhost:*",
                "http://127.0.0.1:*"
            )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(localStorageRoot).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/api/storage/**")
                .addResourceLocations(location);
    }
}
