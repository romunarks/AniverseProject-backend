// VotacionRepository.java - Versión limpia sin duplicados
package com.aniverse.backend.repository;

import com.aniverse.backend.dto.VotacionDTO;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.model.Votacion;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VotacionRepository extends JpaRepository<Votacion, Long> {
    // Métodos básicos
    Optional<Votacion> findByUsuarioAndAnime(Usuario usuario, Anime anime);
    List<Votacion> findByAnime(Anime anime);
    List<Votacion> findByUsuario(Usuario usuario);
    List<Votacion> findByAnimeId(Long id);

    // Métodos con paginación
    List<Votacion> findByUsuario(Usuario usuario, Pageable pageable);

    // Métodos de conteo
    long countByUsuario(Usuario usuario);
    long countByAnimeId(Long animeId);
    Optional<Votacion> findByUsuario_IdAndAnime_JikanId(Long usuario_id, Integer anime_jikanId);

    @Query("SELECT COUNT(v) FROM Votacion v WHERE v.usuario.id = :usuarioId")
    long countByUsuarioId(@Param("usuarioId") Long usuarioId);

    // Métodos de promedio
    @Query("SELECT AVG(v.puntuacion) FROM Votacion v WHERE v.usuario = :usuario")
    Double getPromedioPuntuacionByUsuario(@Param("usuario") Usuario usuario);

    @Query("SELECT AVG(v.puntuacion) FROM Votacion v WHERE v.usuario.id = :usuarioId")
    Double getPromedioPuntuacionByUsuario(@Param("usuarioId") Long usuarioId);

    @Query("SELECT AVG(v.puntuacion) FROM Votacion v")
    Optional<Double> findAveragePuntuacionGlobal();

    // Métodos de verificación
    @Query("SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Votacion v WHERE v.usuario.id = :usuarioId AND v.anime.id = :animeId")
    boolean existsByUsuarioIdAndAnimeId(@Param("usuarioId") Long usuarioId, @Param("animeId") Long animeId);

    // Método para obtener votación específica
    @Query("SELECT v FROM Votacion v WHERE v.usuario.id = :usuarioId AND v.anime.id = :animeId")
    Optional<Votacion> findByUsuarioIdAndAnimeId(@Param("usuarioId") Long usuarioId, @Param("animeId") Long animeId);

    // Método para obtener DTOs
    @Query("SELECT new com.aniverse.backend.dto.VotacionDTO(" +
            "v.id, u.id, u.nombre, a.id, a.titulo, v.puntuacion) " +
            "FROM Votacion v JOIN v.usuario u JOIN v.anime a")
    List<VotacionDTO> findAllAsDTO();
}