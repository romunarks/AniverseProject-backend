package com.aniverse.backend.config;

import com.aniverse.backend.security.JwtAuthenticationFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }
/*
No olvidar cambiar permisos luego!!!!!!
 */
@Bean
public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                    // Endpoints públicos de autenticación
                    .requestMatchers("/ws/**").permitAll()  // Agregar esta línea
                    .requestMatchers("/api/register", "/api/login", "/api/refresh", "/api/logout").permitAll()

                    // Por:
                    .requestMatchers(HttpMethod.GET, "/api/animes/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/animes/save-external").hasAuthority("ROLE_USER") // AGREGAR ESTA LÍNEA
                    .requestMatchers(HttpMethod.GET, "/api/estadisticas/**").permitAll()
                    .requestMatchers(HttpMethod.GET, "/api/recomendaciones/**").permitAll()
                    // Endpoints que requieren autenticación
                    .requestMatchers("/api/listas/**").hasAuthority("ROLE_USER")
                    .requestMatchers("/api/favoritos/**").hasAuthority("ROLE_USER")
                    .requestMatchers("/api/resenyas/**").hasAuthority("ROLE_USER")
                    .requestMatchers("/api/votaciones/**").hasAuthority("ROLE_USER")

                    // Estadísticas pueden ser públicas o requieren usuario
                    .requestMatchers("/api/estadisticas/**").permitAll()
                    .requestMatchers("/api/recomendaciones/**").permitAll()

                    // Cualquier otro endpoint requiere autenticación
                    .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

    return http.build();
}

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:5174"); // Puerto de tu React actual
        configuration.addAllowedOrigin("http://localhost:5173"); // Vite React default
        configuration.addAllowedOrigin("http://localhost:3000"); // Create React App default
        configuration.addAllowedOrigin("http://aniverse-app.com"); // Tu dominio de producción
        configuration.addAllowedMethod("*"); // Todos los métodos
        configuration.addAllowedHeader("*"); // Todos los headers
        configuration.setAllowCredentials(true); // Permitir credenciales (cookies)

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
/**
 *  @Bean
 *     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
 *         // Nueva API para configurar la seguridad en Spring Security 6.1
 *         http
 *                 .csrf(AbstractHttpConfigurer::disable)
 *                 .authorizeHttpRequests(auth -> auth
 *                         .requestMatchers("/api/register", "/api/login", "/api/refresh").permitAll()
 *                         .anyRequest().authenticated()
 *                 )
 *                 .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
 *
 *
 *         return http.build();
 *     }
 */
/**
 * @Bean
 *     public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
 *         // Nueva API para configurar la seguridad en Spring Security 6.1
 *         http
 *                 .csrf(AbstractHttpConfigurer::disable)
 *                 .authorizeHttpRequests(auth -> auth
 *                         .requestMatchers("/api/register", "/api/login").permitAll()
 *                         .requestMatchers("/api/usuarios/**").hasAuthority("ROLE_ADMIN").anyRequest().permitAll() // Requiere ROLE_ADMIN
 *                         .requestMatchers("/api/favoritos/**").hasAuthority("ROLE_USER").anyRequest().permitAll() // Requiere ROLE_USER
 *                         .anyRequest().authenticated()
 *
 *                 )
 *                 .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
 *
 *
 *         return http.build();
 *     }
 */