package com.aniverse.backend.repository;

import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Resenya;
import com.aniverse.backend.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ResenyaRepository extends JpaRepository<Resenya, Long> {

    // ===== MÉTODOS BÁSICOS =====
    boolean existsByUsuarioAndAnime(Usuario usuario, Anime anime);
    Optional<Resenya> findByUsuarioAndAnime(Usuario usuario, Anime anime);
    Page<Resenya> findAll(Pageable pageable);
    List<Resenya> findByAnime(Anime anime);

    // ===== MÉTODOS CON FILTRO DE ELIMINADO =====

    @Query("SELECT r FROM Resenya r WHERE r.eliminado = false")
    Page<Resenya> findAllActive(Pageable pageable);

    @Query("SELECT r FROM Resenya r WHERE r.eliminado = true")
    Page<Resenya> findAllDeleted(Pageable pageable);

    @Query("SELECT r FROM Resenya r WHERE r.id = :id AND r.eliminado = false")
    Optional<Resenya> findByIdAndEliminadoFalse(@Param("id") Long id);

    // ✅ MÉTODOS CORREGIDOS PARA USUARIO CON PAGINACIÓN
    @Query("SELECT r FROM Resenya r WHERE r.usuario = :usuario AND r.eliminado = false ORDER BY r.fechaCreacion DESC")
    Page<Resenya> findByUsuarioAndEliminadoFalse(@Param("usuario") Usuario usuario, Pageable pageable);

    @Query("SELECT r FROM Resenya r WHERE r.usuario.id = :usuarioId AND r.eliminado = false ORDER BY r.fechaCreacion DESC")
    Page<Resenya> findByUsuarioIdAndEliminadoFalse(@Param("usuarioId") Long usuarioId, Pageable pageable);

    // ✅ MÉTODOS CORREGIDOS PARA ANIME CON PAGINACIÓN
    @Query("SELECT r FROM Resenya r WHERE r.anime = :anime AND r.eliminado = false ORDER BY r.fechaCreacion DESC")
    Page<Resenya> findByAnimeAndEliminadoFalse(@Param("anime") Anime anime, Pageable pageable);

    @Query("SELECT r FROM Resenya r WHERE r.anime.id = :animeId AND r.eliminado = false ORDER BY r.fechaCreacion DESC")
    Page<Resenya> findByAnimeIdAndEliminadoFalse(@Param("animeId") Long animeId, Pageable pageable);

    // ✅ MÉTODOS PARA LISTAS (SIN PAGINACIÓN)
    @Query("SELECT r FROM Resenya r WHERE r.anime = :anime AND r.eliminado = false")
    List<Resenya> findByAnimeAndEliminadoFalse(@Param("anime") Anime anime);

    @Query("SELECT r FROM Resenya r WHERE r.usuario = :usuario AND r.eliminado = false")
    List<Resenya> findByUsuarioAndEliminadoFalse(@Param("usuario") Usuario usuario);

    // ===== MÉTODOS DE VERIFICACIÓN =====

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Resenya r WHERE r.usuario = :usuario AND r.anime = :anime AND r.eliminado = false")
    boolean existsByUsuarioAndAnimeAndEliminadoFalse(@Param("usuario") Usuario usuario, @Param("anime") Anime anime);

    @Query("SELECT r FROM Resenya r WHERE r.usuario = :usuario AND r.anime = :anime AND r.eliminado = false")
    Optional<Resenya> findByUsuarioAndAnimeAndEliminadoFalse(@Param("usuario") Usuario usuario, @Param("anime") Anime anime);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM Resenya r WHERE r.id = :resenyaId AND r.usuario.id = :usuarioId AND r.eliminado = false")
    boolean isResenyaOwnedByUser(@Param("resenyaId") Long resenyaId, @Param("usuarioId") Long usuarioId);

    @Query("SELECT r FROM Resenya r WHERE r.id = :id AND r.usuario.id = :usuarioId AND r.eliminado = false")
    Optional<Resenya> findByIdAndUsuarioIdAndEliminadoFalse(@Param("id") Long id, @Param("usuarioId") Long usuarioId);

    // ===== MÉTODOS DE CONTEO =====

    @Query("SELECT COUNT(r) FROM Resenya r WHERE r.usuario.id = :usuarioId AND r.eliminado = false")
    long countByUsuarioIdAndEliminadoFalse(@Param("usuarioId") Long usuarioId);

    @Query("SELECT COUNT(r) FROM Resenya r WHERE r.usuario = :usuario AND r.eliminado = false")
    long countByUsuarioAndEliminadoFalse(@Param("usuario") Usuario usuario);

    @Query("SELECT COUNT(r) FROM Resenya r WHERE r.anime.id = :animeId AND r.eliminado = false")
    long countByAnimeIdAndEliminadoFalse(@Param("animeId") Long animeId);

    // ===== MÉTODOS DE ESTADÍSTICAS =====

    @Query("SELECT AVG(r.puntuacion) FROM Resenya r WHERE r.anime.id = :animeId AND r.eliminado = false")
    Double findAveragePuntuacionByAnimeId(@Param("animeId") Long animeId);

    @Query("SELECT AVG(r.puntuacion) FROM Resenya r WHERE r.anime.jikanId = :jikanId AND r.eliminado = false")
    Double findAveragePuntuacionByAnimeJikanId(@Param("jikanId") Long jikanId);

    @Query("SELECT MAX(r.puntuacion) FROM Resenya r WHERE r.anime.id = :animeId AND r.eliminado = false")
    Double findMaxPuntuacionByAnimeId(@Param("animeId") Long animeId);

    @Query("SELECT MIN(r.puntuacion) FROM Resenya r WHERE r.anime.id = :animeId AND r.eliminado = false")
    Double findMinPuntuacionByAnimeId(@Param("animeId") Long animeId);



    // ===== MÉTODOS AVANZADOS PARA REPORTES =====

    @Query("SELECT r.puntuacion, COUNT(r) FROM Resenya r WHERE r.anime.id = :animeId AND r.eliminado = false GROUP BY r.puntuacion ORDER BY r.puntuacion")
    List<Object[]> findPuntuacionDistributionByAnimeId(@Param("animeId") Long animeId);

    @Query("SELECT YEAR(r.fechaCreacion), MONTH(r.fechaCreacion), COUNT(r) FROM Resenya r WHERE r.usuario.id = :usuarioId AND r.eliminado = false GROUP BY YEAR(r.fechaCreacion), MONTH(r.fechaCreacion) ORDER BY YEAR(r.fechaCreacion) DESC, MONTH(r.fechaCreacion) DESC")
    List<Object[]> findResenyasPorMesByUsuario(@Param("usuarioId") Long usuarioId);

    @Query("SELECT r FROM Resenya r WHERE r.eliminado = false ORDER BY r.fechaCreacion DESC")
    List<Resenya> findRecentResenyas(Pageable pageable);
    // ✅ MÉTODO CRÍTICO PARA EVITAR LAZY LOADING
    @Query("SELECT r FROM Resenya r " +
            "LEFT JOIN FETCH r.usuario u " +
            "LEFT JOIN FETCH r.anime a " +
            "WHERE r.usuario.id = :usuarioId AND r.eliminado = false " +
            "ORDER BY r.fechaCreacion DESC")
    List<Resenya> findByUsuarioIdWithDetails(@Param("usuarioId") Long usuarioId);
    // También asegúrate de que tienes este método básico:
    List<Resenya> findByUsuarioId(Long usuarioId);
    @Query("SELECT r FROM Resenya r WHERE r.anime.id = :animeId AND r.eliminado = false ORDER BY r.puntuacion DESC")
    List<Resenya> findTopResenyasByAnime(@Param("animeId") Long animeId, Pageable pageable);
}