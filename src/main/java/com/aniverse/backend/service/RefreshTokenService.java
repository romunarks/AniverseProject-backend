package com.aniverse.backend.service;

import com.aniverse.backend.model.RefreshToken;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.repository.RefreshTokenRepository;
import com.aniverse.backend.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${jwt.refreshExpiration}") // 🔹 Cargamos el tiempo de expiración desde application.properties
    private long refreshExpirationMillis;

    // contructor...
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, UsuarioRepository usuarioRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public RefreshToken createRefreshToken(Usuario usuario) {
        // 🔹 Eliminar el refresh token anterior si ya existe para este usuario
        refreshTokenRepository.findByUsuario(usuario).ifPresent(refreshTokenRepository::delete);

        // 🔹 Crear un nuevo refresh token
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUsuario(usuario);
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setExpiracion(Instant.now().plusMillis(refreshExpirationMillis)); // 🔹 Ahora usa application.properties

        return refreshTokenRepository.save(refreshToken);
    }

    // Buscar un refresh token válido
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }

    // Eliminar un refresh token cuando un usuario cierra sesión
    @Transactional // 🔹 Esta anotación permite eliminar datos correctamente
    public void deleteByUsuario(Usuario usuario) {
        refreshTokenRepository.deleteByUsuario(usuario);
    }
}
