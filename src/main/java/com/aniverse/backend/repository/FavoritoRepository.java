package com.aniverse.backend.repository;

import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Favorito;
import com.aniverse.backend.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FavoritoRepository extends JpaRepository<Favorito, Long> {

    // ===============================================
    // MÉTODOS BÁSICOS DE FAVORITOS
    // ===============================================

    boolean existsByUsuarioAndAnime(Usuario usuario, Anime anime);
    Optional<Favorito> findByUsuarioAndAnime(Usuario usuario, Anime anime);
    List<Favorito> findByUsuario(Usuario usuario);
    Page<Favorito> findByUsuario(Usuario usuario, Pageable pageable);
    List<Favorito> findByAnime(Anime anime);
    long countByUsuario(Usuario usuario);

    // ===============================================
    // MÉTODOS POR ID (MEJORADOS - USANDO LONG)
    // ===============================================

    /**
     * Verifica si existe un favorito por usuario ID y Jikan ID del anime
     * CORREGIDO: Ahora usa Long en lugar de Integer
     */
    boolean existsByUsuario_IdAndAnime_JikanId(Long usuarioId, Long animeJikanId);

    /**
     * Busca un favorito específico por usuario ID y Jikan ID del anime
     * CORREGIDO: Ahora usa Long en lugar de Integer
     */
    Optional<Favorito> findByUsuario_IdAndAnime_JikanId(Long usuarioId, Long animeJikanId);

    // ===============================================
    // MÉTODOS DE CONSULTA AVANZADA
    // ===============================================

    @Query("SELECT COUNT(f) FROM Favorito f WHERE f.usuario.id = :usuarioId")
    long countByUsuarioId(@Param("usuarioId") Long usuarioId);

    @Query("SELECT f.anime.genero, COUNT(f) FROM Favorito f " +
            "WHERE f.usuario.id = :usuarioId AND f.anime.genero IS NOT NULL " +
            "GROUP BY f.anime.genero ORDER BY COUNT(f) DESC")
    List<Object[]> findGenerosFavoritosByUsuario(@Param("usuarioId") Long usuarioId);

    @Query("SELECT f.anime.titulo FROM Favorito f " +
            "WHERE f.usuario.id = :usuarioId " +
            "ORDER BY f.fechaAgregado DESC")
    List<String> findTitulosAnimeFavoritosByUsuario(@Param("usuarioId") Long usuarioId, Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Favorito f " +
            "WHERE f.usuario.id = :usuarioId AND f.anime.id = :animeId")
    boolean existsByUsuarioIdAndAnimeId(@Param("usuarioId") Long usuarioId, @Param("animeId") Long animeId);

    // ===============================================
    // MÉTODOS PARA ESTADÍSTICAS Y RECOMENDACIONES
    // ===============================================

    /**
     * Obtiene los géneros más populares entre todos los usuarios
     */
    @Query("SELECT a.genero, COUNT(f) as count FROM Favorito f " +
            "JOIN f.anime a WHERE a.genero IS NOT NULL " +
            "GROUP BY a.genero ORDER BY count DESC")
    List<Object[]> findGenerosPopulares(Pageable pageable);

    /**
     * Obtiene los animes más añadidos a favoritos
     */
    @Query("SELECT a, COUNT(f) as count FROM Favorito f " +
            "JOIN f.anime a " +
            "GROUP BY a ORDER BY count DESC")
    List<Object[]> findAnimesMasPopulares(Pageable pageable);

    /**
     * Encuentra usuarios con gustos similares basado en animes favoritos comunes
     */
    @Query("SELECT f2.usuario, COUNT(f2) as animesComunes FROM Favorito f1 " +
            "JOIN Favorito f2 ON f1.anime = f2.anime " +
            "WHERE f1.usuario.id = :usuarioId AND f2.usuario.id != :usuarioId " +
            "GROUP BY f2.usuario ORDER BY animesComunes DESC")
    List<Object[]> findUsuariosConGustosSimilares(@Param("usuarioId") Long usuarioId, Pageable pageable);

    // ===============================================
    // MÉTODOS PARA VALIDACIONES DE RESEÑAS
    // ===============================================

    /**
     * Verifica si un usuario tiene un anime específico en favoritos (para permitir reseñas)
     */
    @Query("SELECT CASE WHEN COUNT(f) > 0 THEN true ELSE false END FROM Favorito f " +
            "WHERE f.usuario.id = :usuarioId AND (f.anime.id = :animeId OR f.anime.jikanId = :jikanId)")
    boolean puedeResenarAnime(@Param("usuarioId") Long usuarioId,
                              @Param("animeId") Long animeId,
                              @Param("jikanId") Long jikanId);

    /**
     * Obtiene todos los animes favoritos de un usuario (para dropdown de reseñas)
     */
    @Query("SELECT f.anime FROM Favorito f " +
            "WHERE f.usuario.id = :usuarioId AND f.anime.eliminado = false " +
            "ORDER BY f.fechaAgregado DESC")
    List<Anime> findAnimesFavoritosByUsuario(@Param("usuarioId") Long usuarioId);

    // ===============================================
    // MÉTODOS PARA MÉTRICAS Y DASHBOARD
    // ===============================================

    /**
     * Cuenta favoritos por mes (para estadísticas)
     */
    @Query("SELECT YEAR(f.fechaAgregado), MONTH(f.fechaAgregado), COUNT(f) " +
            "FROM Favorito f WHERE f.usuario.id = :usuarioId " +
            "GROUP BY YEAR(f.fechaAgregado), MONTH(f.fechaAgregado) " +
            "ORDER BY YEAR(f.fechaAgregado) DESC, MONTH(f.fechaAgregado) DESC")
    List<Object[]> findFavoritosPorMes(@Param("usuarioId") Long usuarioId);
}