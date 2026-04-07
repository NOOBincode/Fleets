package org.example.fleets.common.config;

import java.io.File;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    @Value("${file.upload.path:upload}")
    private String uploadPath;
    
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }
    
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File uploadDir = new File(uploadPath);
        if (!uploadDir.isAbsolute()) {
            uploadDir = new File(System.getProperty("user.dir"), uploadPath);
        }
        
        String location = uploadDir.getAbsolutePath();
        if (!location.endsWith("/") && !location.endsWith("\\")) {
            location = location + "/";
        }
        location = "file:///" + location.replace("\\", "/");
        
        log.info("静态资源映射: /files/** -> {}", location);
        
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location);
    }
}
