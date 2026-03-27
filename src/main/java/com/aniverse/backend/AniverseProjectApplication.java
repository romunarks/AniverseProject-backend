package com.aniverse.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableCaching
@EnableScheduling  // Añadir esta anotación
@EnableAsync  // ← Añadir esta anotación
public class AniverseProjectApplication {

    public static void main(String[] args) {
        SpringApplication.run(AniverseProjectApplication.class, args);
    }

}
