package com.aniverse.backend.repository;

import com.aniverse.backend.model.RefreshToken;
import com.aniverse.backend.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
  Optional<RefreshToken> findByToken(String token); // Buscar refresh token por token
  Optional<RefreshToken> findByUsuario(Usuario usuario); // Buscar refresh token por usuario
  void deleteByUsuario(Usuario usuario); // Eliminar refresh token por usuario
  }// }