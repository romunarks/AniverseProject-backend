package com.aniverse.backend.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenAPIConfig {

    @Value("${server.port}")
    private String serverPort;

    @Bean
    public OpenAPI aniVerseOpenAPI() {
        Server localServer = new Server()
                .url("http://localhost:" + serverPort)
                .description("Servidor de desarrollo local");

        Contact contact = new Contact()
                .name("Rodrigo")
                .email("tu@email.com");

        Info info = new Info()
                .title("Aniverse API")
                .version("1.0")
                .description("API RESTful para la aplicación Aniverse - Gestión de animes, reseñas y usuarios")
                .contact(contact);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}