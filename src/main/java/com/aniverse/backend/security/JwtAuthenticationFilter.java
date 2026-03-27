package com.aniverse.backend.security;

import com.aniverse.backend.repository.UsuarioRepository; // ✅ REPOSITORY DIRECTO
import org.springframework.lang.NonNull;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UsuarioRepository usuarioRepository; // ✅ REPOSITORY EN LUGAR DE SERVICE

    // ✅ CONSTRUCTOR SIN DEPENDENCIA CIRCULAR
    public JwtAuthenticationFilter(JwtUtil jwtUtil, UsuarioRepository usuarioRepository) {
        this.jwtUtil = jwtUtil;
        this.usuarioRepository = usuarioRepository;
    }

    /**
     * Método principal que intercepta las solicitudes y valida el token JWT.
     */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        System.out.println("🔍 Processing request URI: " + requestURI + " [" + request.getMethod() + "]");

        // ✅ RUTAS PÚBLICAS EXPANDIDAS - INCLUYE ESTADÍSTICAS
        String[] publicRoutes = {
                "/api/register",
                "/api/login",
                "/api/refresh",
                "/api/logout",
                "/api/estadisticas",           // ✅ AGREGADO: Estadísticas generales
                "/api/estadisticas/top-rated", // ✅ AGREGADO: Top rated animes
                "/api/estadisticas/most-recent", // ✅ AGREGADO: Animes recientes
                "/api/estadisticas/most-voted"   // ✅ AGREGADO: Animes más votados
        };

        for (String route : publicRoutes) {
            if (requestURI.equals(route)) {
                System.out.println("✅ Public route detected: " + route);
                filterChain.doFilter(request, response);
                return;
            }
        }

        // ✅ RUTAS PÚBLICAS POR MÉTODO - GET requests específicos
        if (request.getMethod().equals("GET")) {
            String[] publicGetRoutes = {
                    "/api/animes",
                    "/api/animes/home",
                    "/api/animes/trending",
                    "/api/animes/recent",
                    "/api/animes/top-rated",
                    "/api/animes/external/featured"
            };

            for (String route : publicGetRoutes) {
                if (requestURI.equals(route)) {
                    System.out.println("✅ Public GET route detected: " + route);
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        }
        // Leer el encabezado "Authorization" de la solicitud
        String authHeader = request.getHeader("Authorization");
        System.out.println("🔑 Authorization header: " + (authHeader != null ? "Present" : "Not present"));

        // Validar que el encabezado no sea nulo y comience con "Bearer "
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // Extraer el token después de "Bearer "
            System.out.println("🎫 Token extracted, validating...");

            try {
                // Validar el token
                if (jwtUtil.validateToken(token)) {
                    System.out.println("✅ Token valid!");
                    var claims = jwtUtil.extractClaims(token);
                    String username = (String) claims.get("sub"); // Email del usuario
                    String role = (String) claims.get("role"); // Rol del usuario
                    System.out.println("👤 User: " + username + ", Role: " + role);

                    // ✅ BUSCAR EL USER ID POR EMAIL USANDO REPOSITORY DIRECTO
                    try {
                        var usuarioOpt = usuarioRepository.findByEmailAndEliminadoFalse(username);
                        if (usuarioOpt.isPresent()) {
                            var usuario = usuarioOpt.get();

                            // ✅ AGREGAR userId AL REQUEST para que el controller lo pueda usar
                            request.setAttribute("userId", usuario.getId());
                            request.setAttribute("userEmail", usuario.getEmail());
                            request.setAttribute("userName", usuario.getNombre());

                            System.out.println("✅ User data added to request - ID: " + usuario.getId() + ", Name: " + usuario.getNombre());
                        } else {
                            System.out.println("❌ Usuario no encontrado en BD para email: " + username);
                            throw new RuntimeException("Usuario no encontrado");
                        }
                    } catch (Exception e) {
                        System.out.println("❌ Error obteniendo datos del usuario: " + e.getMessage());
                        throw new RuntimeException("Error obteniendo datos del usuario: " + e.getMessage());
                    }

                    // Agregar prefijo "ROLE_" si es necesario
                    String prefixedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                    // Crear la lista de autoridades para Spring Security
                    var authorities = List.of(new SimpleGrantedAuthority(prefixedRole));

                    // Crear el objeto de autenticación
                    var authentication = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    System.out.println("✅ Authentication configured successfully for: " + username);
                } else {
                    throw new RuntimeException("Invalid JWT token");
                }
            } catch (Exception e) {
                // ✅ MANEJO DE ERRORES MEJORADO
                System.out.println("❌ JWT Validation Error: " + e.getMessage());
                System.out.println("❌ Request URI: " + requestURI);
                System.out.println("❌ Token (first 20 chars): " + (token.length() > 20 ? token.substring(0, 20) + "..." : token));

                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.setCharacterEncoding("UTF-8");
                response.getWriter().write(
                        "{\"success\": false, \"message\": \"Token inválido: " + e.getMessage() + "\", \"data\": null}"
                );
                return;
            }
        } else {
            // ✅ NO HAY TOKEN - Verificar si el endpoint requiere autenticación
            System.out.println("❌ No Authorization header found for protected endpoint: " + requestURI);

            // Para endpoints protegidos sin token, devolver 401
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write(
                    "{\"success\": false, \"message\": \"Token de autorización requerido\", \"data\": null}"
            );
            return;
        }

        // ✅ Continuar con el resto de los filtros
        System.out.println("✅ Proceeding to next filter for: " + requestURI);
        filterChain.doFilter(request, response);
    }
}