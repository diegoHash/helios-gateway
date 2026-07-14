package com.helios.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@SpringBootApplication
public class HeliosGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(HeliosGatewayApplication.class, args);
    }

    @Bean
    public CorsConfiguration heliosCorsConfiguration() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOriginPatterns(java.util.Arrays.asList(
                "https://helios-platform.site",
                "https://*.helios-platform.site",
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));
        corsConfig.setMaxAge(3600L);
        corsConfig.setAllowedMethods(java.util.Arrays.asList(
                "GET",
                "HEAD",
                "POST",
                "PUT",
                "PATCH",
                "DELETE",
                "OPTIONS"
        ));
        corsConfig.addAllowedHeader("*");
        corsConfig.setExposedHeaders(java.util.Arrays.asList(
                "Authorization",
                "Content-Length",
                "Content-Type"
        ));
        corsConfig.setAllowCredentials(true);

        return corsConfig;
    }

    @Bean
    public CorsWebFilter corsWebFilter(CorsConfiguration heliosCorsConfiguration) {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", heliosCorsConfiguration);
        return new CorsWebFilter(source);
    }
}
