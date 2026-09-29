package com.insurance.assistant.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Allows the separately-hosted frontend (frontend/) to call this API during
// local development. Only one configurable origin - not "*" - since this API
// will eventually require authentication.
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${FRONTEND_ORIGIN:http://localhost:5500}")
    private String frontendOrigin;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(frontendOrigin)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
