package com.aniverse.backend.service;

import com.aniverse.backend.dto.*;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Votacion;
import com.aniverse.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EstadisticaService {

    private static final Logger log = LoggerFactory.getLogger(EstadisticaService.class);

    private final AnimeRepository animeRepository;
    private final UsuarioRepository usuarioRepository;
    private final FavoritoRepository favoritoRepository;
    private final VotacionRepository votacionRepository;
    private final ResenyaRepository resenyaRepository; // ✅ AGREGADO

    public EstadisticaService(AnimeRepository animeRepository,
                              UsuarioRepository usuarioRepository,
                              FavoritoRepository favoritoRepository,
                              VotacionRepository votacionRepository,
                              ResenyaRepository resenyaRepository) { // ✅ AGREGADO
        this.animeRepository = animeRepository;
        this.usuarioRepository = usuarioRepository;
        this.favoritoRepository = favoritoRepository;
        this.votacionRepository = votacionRepository;
        this.resenyaRepository = resenyaRepository; // ✅ AGREGADO
    }

    public Map<String, Object> getEstadisticasAnime(Long animeId) {
        try {
            // ✅ USAR ResenyaRepository para estadísticas reales
            long totalResenyas = resenyaRepository.countByAnimeIdAndEliminadoFalse(animeId);
            Double promedioPuntuacion = resenyaRepository.findAveragePuntuacionByAnimeId(animeId);

            return Map.of(
                    "totalResenyas", totalResenyas,
                    "promedioPuntuacion", promedioPuntuacion != null ? promedioPuntuacion : 0.0,
                    "totalVotaciones", totalResenyas,
                    "promedioVotaciones", promedioPuntuacion != null ? promedioPuntuacion : 0.0
            );
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas del anime {}: {}", animeId, e.getMessage());
            return Map.of(
                    "totalResenyas", 0L,
                    "promedioPuntuacion", 0.0,
                    "totalVotaciones", 0L,
                    "promedioVotaciones", 0.0
            );
        }
    }

    public EstadisticasDTO getEstadisticasGenerales() {
        log.debug("Obteniendo estadísticas generales");
        EstadisticasDTO estadisticas = new EstadisticasDTO();

        try {
            // ✅ USAR MÉTODOS CORRECTOS
            long totalAnimes = animeRepository.countTotalAnimes();
            estadisticas.setTotalAnimes(totalAnimes);

            long totalUsuarios = usuarioRepository.count();
            estadisticas.setTotalUsuarios(totalUsuarios);

            // ✅ CONTAR RESEÑAS REALES - NO animes
            long totalResenyas = resenyaRepository.count();
            estadisticas.setTotalResenyas(totalResenyas);

            // Votaciones y puntuación promedio
            long totalVotaciones = votacionRepository.count();
            estadisticas.setTotalVotaciones(totalVotaciones);

            // Promedio de puntuación global
            double promedioPuntuacion = votacionRepository.findAveragePuntuacionGlobal()
                    .orElse(0.0);
            estadisticas.setPuntuacionPromedio(Math.round(promedioPuntuacion * 100.0) / 100.0);

            // Distribución por géneros
            List<Object[]> genreDistribution = animeRepository.getGenreDistribution();
            List<GeneroEstadisticaDTO> generos = new ArrayList<>();

            if (totalAnimes > 0) {
                for (Object[] result : genreDistribution) {
                    String genero = (String) result[0];
                    if (genero == null || genero.trim().isEmpty()) {
                        genero = "Sin clasificar";
                    }

                    Long cantidad = (Long) result[1];
                    double porcentaje = (double) cantidad / totalAnimes * 100;
                    porcentaje = Math.round(porcentaje * 10.0) / 10.0;

                    generos.add(new GeneroEstadisticaDTO(genero, cantidad, porcentaje));
                }
            } else {
                log.warn("No hay animes activos para calcular la distribución de géneros.");
            }
            estadisticas.setDistribucionGeneros(generos);

            // Distribución por años
            List<Object[]> yearDistribution = animeRepository.getYearDistribution();
            List<AnyoEstadisticaDTO> anyos = new ArrayList<>();

            for (Object[] result : yearDistribution) {
                Integer anyo = (result[0] instanceof Number) ? ((Number) result[0]).intValue() : null;
                Long cantidad = (result[1] instanceof Number) ? ((Number) result[1]).longValue() : 0L;

                if (anyo != null) {
                    anyos.add(new AnyoEstadisticaDTO(anyo, cantidad));
                }
            }
            estadisticas.setDistribucionAnyos(anyos);

            log.debug("Estadísticas generales obtenidas con éxito - Animes: {}, Usuarios: {}, Reseñas: {}",
                    totalAnimes, totalUsuarios, totalResenyas);

        } catch (Exception e) {
            log.error("Error obteniendo estadísticas generales: {}", e.getMessage());
            // Devolver estadísticas por defecto en caso de error
            estadisticas.setTotalAnimes(0L);
            estadisticas.setTotalUsuarios(0L);
            estadisticas.setTotalResenyas(0L);
            estadisticas.setTotalVotaciones(0L);
            estadisticas.setPuntuacionPromedio(0.0);
            estadisticas.setDistribucionGeneros(new ArrayList<>());
            estadisticas.setDistribucionAnyos(new ArrayList<>());
        }

        return estadisticas;
    }

    // ✅ SIN ANOTACIONES @Cacheable
    public List<AnimeDTO> getTopRatedAnimes(int limit) {
        log.debug("Obteniendo top {} animes puntuados", limit);
        try {
            List<AnimeDTO> result = animeRepository.findTopRatedAnimesAsDTO(PageRequest.of(0, limit));
            log.debug("Top rated animes obtenidos: {} encontrados", result.size());
            return result;
        } catch (Exception e) {
            log.error("Error obteniendo top rated animes: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ✅ SIN ANOTACIONES @Cacheable
    public List<AnimeDTO> getMostRecentAnimes(int limit) {
        log.debug("Obteniendo {} animes más recientes", limit);
        try {
            List<AnimeDTO> result = animeRepository.findMostRecentAnimesAsDTO(PageRequest.of(0, limit));
            log.debug("Recent animes obtenidos: {} encontrados", result.size());
            return result;
        } catch (Exception e) {
            log.error("Error obteniendo animes recientes: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    // ✅ SIN ANOTACIONES @Cacheable
    public List<AnimeDTO> getMostVotedAnimes(int limit) {
        log.debug("Obteniendo {} animes más votados", limit);
        try {
            List<AnimeDTO> result = animeRepository.findMostVotedAnimesAsDTO(PageRequest.of(0, limit));
            log.debug("Most voted animes obtenidos: {} encontrados", result.size());
            return result;
        } catch (Exception e) {
            log.error("Error obteniendo animes más votados: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
}