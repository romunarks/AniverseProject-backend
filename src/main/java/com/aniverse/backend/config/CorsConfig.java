package com.aniverse.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.Collections;

@Configuration
public class CorsConfig {

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                        .allowedOrigins(
                                "http://localhost:5174",  // Puerto actual de tu React
                                "http://localhost:5173",  // Vite React default
                                "http://localhost:3000",  // Create React App default
                                "http://localhost:4200",  // Angular default por si lo usas en el futuro
                                "http://aniverse-app.com" // Producción (cambiar a tu dominio real)
                        )
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
                        .allowedHeaders("*")
                        .allowCredentials(true)
                        .maxAge(3600); // 1 hora
            }
        };
    }

    // Configuración alternativa usando CorsFilter (necesario en algunos casos con Spring Security)
    @Bean
    public CorsFilter corsFilter() {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        final CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(Arrays.asList(
                "http://localhost:5174", // Puerto actual
                "http://localhost:5173",
                "http://localhost:3000",
                "http://localhost:4200",
                "http://aniverse-app.com"
        ));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowCredentials(true);
        config.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization"));
        // Agregar headers específicos para WebSocket
        config.setExposedHeaders(Arrays.asList(
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "Sec-WebSocket-Protocol",
                "Sec-WebSocket-Accept",
                "Sec-WebSocket-Key"
        ));

        config.setMaxAge(3600L);

        source.registerCorsConfiguration("/**", config);
        source.registerCorsConfiguration("/ws/**", config);  // Agregar esta línea
        return new CorsFilter(source);
    }
}
