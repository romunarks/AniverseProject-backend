package com.aniverse.backend.repository;

import com.aniverse.backend.dto.AnimeDTO;
import com.aniverse.backend.dto.AnimeDetalleDTO; // Asumo que existe
import com.aniverse.backend.model.Anime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AnimeRepository extends JpaRepository<Anime, Long> {

    // --- Métodos que devuelven Entidades Anime (para otros usos) ---
    @Query("SELECT a FROM Anime a WHERE LOWER(a.titulo) LIKE LOWER(CONCAT('%', :titulo, '%')) AND a.eliminado = false")
    Page<Anime> findByTituloContainingIgnoreCase(@Param("titulo") String titulo, Pageable pageable);

    @Query("SELECT a FROM Anime a WHERE LOWER(a.genero) LIKE LOWER(CONCAT('%', :genero, '%')) AND a.eliminado = false")
    Page<Anime> findByGeneroContainingIgnoreCase(@Param("genero") String genero, Pageable pageable);

    @Query("SELECT a FROM Anime a WHERE a.anyo = :anyo AND a.eliminado = false")
    Page<Anime> findByAnyo(@Param("anyo") int anyo, Pageable pageable);

    @Query("SELECT a FROM Anime a WHERE " +
            "(:titulo IS NULL OR LOWER(a.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))) AND " +
            "(:genero IS NULL OR LOWER(a.genero) LIKE LOWER(CONCAT('%', :genero, '%'))) AND " +
            "(:anyo IS NULL OR a.anyo = :anyo) AND a.eliminado = false") // Añadido eliminado = false
    Page<Anime> findByFilters(
            @Param("titulo") String titulo,
            @Param("genero") String genero,
            @Param("anyo") Integer anyo,
            Pageable pageable);

    @Query("SELECT a FROM Anime a WHERE a.id = :id AND a.eliminado = false")
    Optional<Anime> findByIdAndEliminadoFalse(@Param("id") Long id);

    @Query("SELECT a FROM Anime a WHERE a.eliminado = false")
    Page<Anime> findAllActive(Pageable pageable);

    // ✅ AGREGAR ESTOS MÉTODOS DE ESTADÍSTICAS:
    @Query("SELECT COUNT(a) FROM Anime a WHERE a.eliminado = false")
    long countTotalAnimes();

    @Query("SELECT COUNT(DISTINCT a.genero) FROM Anime a WHERE a.eliminado = false")
    long countDistinctGenres();

    @Query("SELECT COUNT(a) FROM Anime a WHERE a.anyo = ?1 AND a.eliminado = false")
    long countAnimesByYear(int year);

    // Búsqueda filtrada, solo no eliminados (esta parece ser la misma que findByFilters si siempre es con eliminados=false)
    // Considera unificarla con findByFilters si la lógica es idéntica
    @Query("SELECT a FROM Anime a WHERE " +
            "a.eliminado = false AND " +
            "(:titulo IS NULL OR LOWER(a.titulo) LIKE LOWER(CONCAT('%', :titulo, '%'))) AND " +
            "(:genero IS NULL OR LOWER(a.genero) LIKE LOWER(CONCAT('%', :genero, '%'))) AND " +
            "(:anyo IS NULL OR a.anyo = :anyo)")
    Page<Anime> findByFiltersActive(
            @Param("titulo") String titulo,
            @Param("genero") String genero,
            @Param("anyo") Integer anyo,
            Pageable pageable);
    // En AnimeRepository.java
    @Query("SELECT a FROM Anime a " +
            "WHERE (:genero IS NULL OR LOWER(a.genero) LIKE LOWER(CONCAT('%', :genero, '%'))) " +
            "ORDER BY (SELECT AVG(v.puntuacion) FROM Votacion v WHERE v.anime = a) DESC NULLS LAST")
    List<Anime> findTopRatedAnimesByGenre(@Param("genero") String genero, Pageable pageable);
    // Animes mejor puntuados (Devuelve Entidad, usado por EstadisticaService antes de la proyección)
    // Mantenemos esta si EstadisticaService aún la usa y luego convierte, pero idealmente EstadisticaService usaría findTopRatedAnimesAsDTO
    @Query("SELECT a FROM Anime a JOIN a.votaciones v WHERE a.eliminado = false " +
            "GROUP BY a.id ORDER BY AVG(v.puntuacion) DESC")
    List<Anime> findTopRatedAnimes(Pageable pageable); // Esta es la que causaba LazyInit si no se manejaba en el servicio

    @Query("SELECT a.genero, COUNT(a.id) FROM Anime a WHERE a.eliminado = false " + // Corregido COUNT(a) a COUNT(a.id) por claridad
            "GROUP BY a.genero ORDER BY COUNT(a.id) DESC")
    List<Object[]> getGenreDistribution();

    @Query("SELECT a.anyo, COUNT(a.id) FROM Anime a WHERE a.eliminado = false " + // Corregido COUNT(a) a COUNT(a.id)
            "GROUP BY a.anyo ORDER BY a.anyo")
    List<Object[]> getYearDistribution();

    // Animes más recientes (Devuelve Entidad)
    @Query("SELECT a FROM Anime a WHERE a.eliminado = false ORDER BY a.createdAt DESC")
    List<Anime> findMostRecentAnimes(Pageable pageable);

    // Animes más votados (Devuelve Entidad)
    @Query("SELECT a FROM Anime a JOIN a.votaciones v WHERE a.eliminado = false " +
            "GROUP BY a.id ORDER BY COUNT(v) DESC")
    List<Anime> findMostVotedAnimes(Pageable pageable);


    @Query("SELECT a FROM Anime a LEFT JOIN FETCH a.votaciones WHERE a.id = :id AND a.eliminado = false")
    Optional<Anime> findByIdWithVotaciones(@Param("id") Long id);

    Optional<Anime> findByJikanId(Long jikanId); // Puede encontrar eliminados, usar con cuidado

    @Query("SELECT a FROM Anime a LEFT JOIN FETCH a.votaciones WHERE a.jikanId = :jikanId AND a.eliminado = false")
    Optional<Anime> findByJikanIdAndEliminadoFalseWithVotaciones(@Param("jikanId") Long jikanId);

    @Query("SELECT a FROM Anime a WHERE a.jikanId = :jikanId AND a.eliminado = false")
    Optional<Anime> findByJikanIdAndEliminadoFalse(@Param("jikanId") Long jikanId); // Renombrado de findActiveByJikanId para consistencia


    // --- Métodos que devuelven DTOs (para EstadisticaService) ---


    // Reemplaza las líneas 74-96 en tu AnimeRepository.java con estas consultas:
// Reemplaza las líneas 74-96 en tu AnimeRepository.java con estas consultas:

    @Query("SELECT new com.aniverse.backend.dto.AnimeDTO(" +
            "a.id, a.jikanId, a.titulo, a.descripcion, a.genero, a.imagenUrl) " +
            "FROM Anime a WHERE a.eliminado = false ORDER BY a.createdAt DESC")
    List<AnimeDTO> findMostRecentAnimesAsDTO(Pageable pageable);

    @Query("SELECT new com.aniverse.backend.dto.AnimeDTO(" +
            "a.id, a.jikanId, a.titulo, a.descripcion, a.genero, a.imagenUrl) " +
            "FROM Anime a WHERE a.eliminado = false")
    List<AnimeDTO> findTopRatedAnimesAsDTO(Pageable pageable);

    @Query("SELECT new com.aniverse.backend.dto.AnimeDTO(" +
            "a.id, a.jikanId, a.titulo, a.descripcion, a.genero, a.imagenUrl) " +
            "FROM Anime a WHERE a.eliminado = false")
    List<AnimeDTO> findMostVotedAnimesAsDTO(Pageable pageable);

    long countByEliminadoFalse();
    // En AnimeRepository.java
    @Query("SELECT a FROM Anime a " +
            "WHERE (:genero IS NULL OR LOWER(a.genero) LIKE LOWER(CONCAT('%', :genero, '%'))) " +
            "AND a.id <> :animeId " + // Importante: el parámetro se llama animeId aquí
            "ORDER BY (SELECT COUNT(v) FROM Votacion v WHERE v.anime = a) DESC, a.anyo DESC")
    List<Anime> getAnimesSimilares(@Param("animeId") Long animeId, @Param("genero") String genero, Pageable pageable);
    // Si AnimeDetalleDTO tiene un constructor compatible:
    // @Query("SELECT new com.aniverse.backend.dto.AnimeDetalleDTO(" +
    // "a.id, a.titulo, a.descripcion, a.genero, a.imagenUrl, " +
    // "a.createdAt, a.updatedAt, a.createdBy, a.updatedBy) " +
    // "FROM Anime a WHERE a.id = :id AND a.eliminado = false")
    // Optional<AnimeDetalleDTO> findDetalleById(@Param("id") Long id);
}