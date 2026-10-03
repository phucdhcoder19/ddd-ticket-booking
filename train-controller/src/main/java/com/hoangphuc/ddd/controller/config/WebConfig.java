package com.hoangphuc.ddd.controller.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.method.HandlerTypePredicate;

/**
 * HTTP layer configuration. Lives in train-controller rather than
 * train-infrastructure, by this test: "if MySQL were swapped for MongoDB,
 * would this file change?" — No. But switching from HTTP to Kafka would make
 * it meaningless. So it belongs to the interface layer, not the storage layer.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * Origin of the web app. Vite switches ports when 5173 is busy (5174, 5180,
     * 5181...) so a pattern is used instead of listing every port.
     */
    @Value("${app.web.allowed-origin-pattern:http://localhost:[*]}")
    private String allowedOriginPattern;

    /**
     * Add the /api prefix to EVERY @RestController.
     *
     * server.servlet.context-path is not used because it would move /actuator
     * to /api/actuator too — Prometheus, configured to scrape
     * /actuator/prometheus, would break. This only touches our controllers;
     * actuator stays where it is.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix("/api", HandlerTypePredicate.forAnnotation(RestController.class));
    }

    /**
     * CORS — the browser blocks every cross-origin request unless the server
     * declares "I allow it". The web app runs on port 5173+, the API on 9999;
     * a different port is a different origin.
     *
     * Only opened for /api/**; /actuator does not need it and should not have it.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(allowedOriginPattern)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .maxAge(3600);   // cache the preflight for 1 hour, saving one OPTIONS request per call
    }
}
