package com.aniverse.backend.controller;

import com.aniverse.backend.dto.UsuarioDTO;
import com.aniverse.backend.dto.UsuarioRegistroDTO;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.model.RefreshToken;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.security.JwtUtil;
import com.aniverse.backend.service.RefreshTokenService;
import com.aniverse.backend.service.UsuarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class AuthController {

    private final UsuarioService usuarioService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;

    public AuthController(UsuarioService usuarioService, JwtUtil jwtUtil, RefreshTokenService refreshTokenService) {
        this.usuarioService = usuarioService;
        this.jwtUtil = jwtUtil;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Endpoint para registrar un nuevo usuario.
     *
     * @param usuarioDTO Datos del usuario (JSON).
     * @return El usuario registrado o un error si el email ya está en uso.
     */
    @PostMapping("/register")
    public ResponseEntity<AniverseResponse<UsuarioDTO>> register(@Valid @RequestBody UsuarioRegistroDTO usuarioDTO) {
        try {
            Usuario nuevoUsuario = usuarioService.saveUsuario(usuarioDTO);
            UsuarioDTO usuarioDTOResponse = new UsuarioDTO(
                    nuevoUsuario.getId(),
                    nuevoUsuario.getNombre(),
                    nuevoUsuario.getEmail(),
                    nuevoUsuario.getRoles()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AniverseResponse.success("Usuario registrado exitosamente", usuarioDTOResponse));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@RequestBody Map<String, String> request) {
        String refreshToken = request.get("refreshToken");

        Optional<RefreshToken> storedToken = refreshTokenService.findByToken(refreshToken);
        if (storedToken.isEmpty() || storedToken.get().getExpiracion().isBefore(Instant.now())) {
            // En lugar de devolver solo un mensaje de texto:
            // return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token inválido o expirado.");

            // Devolver un objeto JSON estructurado:
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(new AniverseResponse<>(false, "Refresh Token inválido o expirado.", null));
        }

        Usuario usuario = storedToken.get().getUsuario();

        // Generar un nuevo JWT y un nuevo refresh token
        String newJwt = jwtUtil.generateToken(usuario.getEmail(), usuario.getRoles().get(0));
        RefreshToken newRefreshToken = refreshTokenService.createRefreshToken(usuario);

        Map<String, String> tokenInfo = Map.of(
                "token", newJwt,
                "refreshToken", newRefreshToken.getToken()
        );

        return ResponseEntity.ok(new AniverseResponse<>(true, "Token renovado exitosamente", tokenInfo));
    }

    @PostMapping("/logout")
    public ResponseEntity<AniverseResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> requestBody) {

        try {
            String refreshToken = requestBody.get("refreshToken");
            if (refreshToken == null || refreshToken.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(AniverseResponse.error("Refresh token es requerido"));
            }

            Optional<RefreshToken> storedToken = refreshTokenService.findByToken(refreshToken);

            if (storedToken.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Refresh token inválido"));
            }

            Usuario usuario = storedToken.get().getUsuario();
            refreshTokenService.deleteByUsuario(usuario);

            // Método success() debe coincidir con el tipo de retorno Void
            return ResponseEntity.ok(
                    AniverseResponse.success("Sesión cerrada exitosamente", null)
            );

        } catch (Exception e) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al cerrar sesión"));
        }
    }

    /**
     * Endpoint para iniciar sesión y obtener un token JWT.
     *
     * @param credentials Credenciales del usuario (JSON con email y contraseña).
     * @return Token JWT si las credenciales son correctas.
     */
    @PostMapping("/login")
    public ResponseEntity<AniverseResponse<Map<String, Object>>> login(@RequestBody Map<String, String> credentials) {
        try {
            Usuario usuario = usuarioService.authenticate(
                    credentials.get("email"),
                    credentials.get("contrasenya")
            );

            if (usuario == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Credenciales incorrectas"));
            }

            // Asegurarse de que el usuario tenga roles
            if (usuario.getRoles() == null || usuario.getRoles().isEmpty()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(AniverseResponse.error("Usuario sin roles asignados"));
            }

            // Usar el primer rol del usuario
            String role = usuario.getRoles().get(0);
            String token = jwtUtil.generateToken(usuario.getEmail(), role);

            // Eliminar el refresh token anterior antes de crear uno nuevo
            refreshTokenService.deleteByUsuario(usuario);
            RefreshToken refreshToken = refreshTokenService.createRefreshToken(usuario);

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("token", token);
            responseData.put("refreshToken", refreshToken.getToken());
            responseData.put("user", new UsuarioDTO(
                    usuario.getId(),
                    usuario.getNombre(),
                    usuario.getEmail(),
                    usuario.getRoles()
            ));

            return ResponseEntity.ok(
                    AniverseResponse.success("Inicio de sesión exitoso", responseData)
            );
        } catch (Exception e) {
            System.err.println("Login Error: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }
}