package com.colisender.api.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private static final String UPLOAD_BASE =
        "file:" + System.getProperty("user.home") + "/colisender_uploads/";

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/profils/**")
                .addResourceLocations(UPLOAD_BASE + "profils/");

        registry.addResourceHandler("/uploads/colis/**")
                .addResourceLocations(UPLOAD_BASE + "colis/");

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(UPLOAD_BASE + "profils/",
                                      UPLOAD_BASE + "colis/");
    }

    // CORS géré uniquement par SecurityConfig.java
    // Ne PAS redéfinir addCorsMappings ici — cela crée un conflit avec
    // le CorsConfigurationSource de Spring Security et provoque l'erreur :
    // "allowedOrigins cannot contain '*' when allowCredentials is true"
}
