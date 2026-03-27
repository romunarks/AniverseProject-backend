package com.aniverse.backend.service;

import com.aniverse.backend.dto.VotacionDTO;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.model.Votacion;
import com.aniverse.backend.repository.AnimeRepository;
import com.aniverse.backend.repository.UsuarioRepository;
import com.aniverse.backend.repository.VotacionRepository;
import com.aniverse.backend.util.ActividadTipo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;


import java.util.*;
import java.util.stream.Collectors;

@Service
public class VotacionService {

    private final VotacionRepository votacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnimeRepository animeRepository;
    private final NotificacionService notificacionService;
    private final ActividadService actividadService;


    public VotacionService(VotacionRepository votacionRepository,
                           UsuarioRepository usuarioRepository,
                           AnimeRepository animeRepository,
                           NotificacionService notificacionService, ActividadService actividadService) {
        this.votacionRepository = votacionRepository;
        this.usuarioRepository = usuarioRepository;
        this.animeRepository = animeRepository;
        this.notificacionService = notificacionService;
        this.actividadService = actividadService;
    }

    public Votacion saveVotacion(Votacion votacion) {
        Usuario usuario = usuarioRepository.findById(votacion.getUsuario().getId())
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + votacion.getUsuario().getId()));

        Anime anime = animeRepository.findById(votacion.getAnime().getId())
                .orElseThrow(() -> new NoSuchElementException("Anime no encontrado con ID: " + votacion.getAnime().getId()));

        if (votacion.getPuntuacion() < 1.0 || votacion.getPuntuacion() > 5.0) {
            throw new IllegalArgumentException("La puntuación debe estar entre 1.0 y 5.0.");
        }

        Optional<Votacion> votacionExistente = votacionRepository.findByUsuarioAndAnime(usuario, anime);
        if (votacionExistente.isPresent()) {
            Votacion existente = votacionExistente.get();
            existente.setPuntuacion(votacion.getPuntuacion());
            return votacionRepository.save(existente);
        }

        votacion.setUsuario(usuario);
        votacion.setAnime(anime);

        Votacion guardada = votacionRepository.save(votacion);

        // Registrar actividad
        actividadService.registrarActividad(
                votacion.getUsuario().getId(),
                ActividadTipo.VALORACION.name(),
                votacion.getAnime().getId(),
                "ANIME",
                Map.of("puntuacion", votacion.getPuntuacion())
        );

        // No es necesario notificar por votaciones individuales ya que podría generar demasiadas notificaciones
        // Alternativamente, podrías notificar solo cuando la puntuación es alta (4 o 5)
        if (votacion.getPuntuacion() >= 4.0) {
            // Notificar a los usuarios que tienen este anime como favorito o al creador del anime
            // Si implementas un sistema donde el anime tiene un creador
        }
        return guardada;
    }

    public VotacionDTO getVotacionUsuarioAnimeByJikanId(Long usuarioId, Integer jikanId) {
        Optional<Votacion> votacion = votacionRepository
                .findByUsuario_IdAndAnime_JikanId(usuarioId, jikanId); // Sin conversión

        return votacion.map(this::convertToDTO).orElse(null);
    }



    /**
     * Obtiene las votaciones de un usuario con paginación
     */
    public List<VotacionDTO> getVotacionesByUsuario(Long usuarioId, int page, int size) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + usuarioId));

        Pageable pageable = PageRequest.of(page, size);
        List<Votacion> votaciones = votacionRepository.findByUsuario(usuario, pageable);

        return votaciones.stream()
                .map(votacion -> new VotacionDTO(
                        votacion.getId(),
                        votacion.getUsuario().getId(),
                        votacion.getUsuario().getNombre(),
                        votacion.getAnime().getId(),
                        votacion.getAnime().getTitulo(),
                        votacion.getPuntuacion()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Obtiene la votación específica de un usuario para un anime
     */
    public VotacionDTO getVotacionUsuarioAnime(Long usuarioId, Long animeId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + usuarioId));

        Anime anime = animeRepository.findById(animeId)
                .orElseThrow(() -> new NoSuchElementException("Anime no encontrado con ID: " + animeId));

        Optional<Votacion> votacion = votacionRepository.findByUsuarioAndAnime(usuario, anime);

        return votacion.map(v -> new VotacionDTO(
                v.getId(),
                v.getUsuario().getId(),
                v.getUsuario().getNombre(),
                v.getAnime().getId(),
                v.getAnime().getTitulo(),
                v.getPuntuacion()
        )).orElse(null);
    }


// AGREGAR ESTE MÉTODO A tu VotacionService.java

    private VotacionDTO convertToDTO(Votacion votacion) {
        if (votacion == null) {
            return null;
        }

        return new VotacionDTO(
                votacion.getId(),
                votacion.getUsuario().getId(),
                votacion.getUsuario().getNombre(),
                votacion.getAnime().getId(),
                votacion.getAnime().getTitulo(),
                votacion.getPuntuacion()
        );
    }


    public void deleteVotacion(Long id) {
        if (!votacionRepository.existsById(id)) {
            throw new NoSuchElementException("No se encontró la votación con ID: " + id);
        }
        votacionRepository.deleteById(id);
    }

    public List<VotacionDTO> getAllVotaciones() {
        return votacionRepository.findAll()
                .stream()
                .map(votacion -> new VotacionDTO(
                        votacion.getId(),
                        votacion.getUsuario().getId(),
                        votacion.getUsuario().getNombre(),
                        votacion.getAnime().getId(),
                        votacion.getAnime().getTitulo(),
                        votacion.getPuntuacion()
                ))
                .collect(Collectors.toList());
    }
// REEMPLAZAR en VotacionService.java

    public long getVotacionesCountByAnime(Long animeId) {
        return votacionRepository.countByAnimeId(animeId);
    }
    /**
     * Crea una nueva votación por jikanId
     */
    public boolean createVotacion(Long userId, Long jikanId, Integer puntuacion) {
        try {
            // Verificar si ya existe votación
            Optional<Votacion> existente = findByUserIdAndJikanId(userId, jikanId);
            if (existente.isPresent()) {
                System.err.println("Ya existe una votación para este usuario y anime");
                return false;
            }

            // Encontrar usuario
            Usuario usuario = usuarioRepository.findById(userId)
                    .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado"));

            // Encontrar anime por jikanId
            Optional<Anime> animeOpt = animeRepository.findByJikanId(jikanId);
            if (animeOpt.isEmpty()) {
                System.err.println("Anime no encontrado con jikanId: " + jikanId);
                return false;
            }

            Anime anime = animeOpt.get();

            // Convertir puntuación de escala 1-10 a 1-5
            Double puntuacionInterna = puntuacion / 2.0;

            // Crear votación
            Votacion votacion = new Votacion();
            votacion.setUsuario(usuario);
            votacion.setAnime(anime);
            votacion.setPuntuacion(puntuacionInterna);

            Votacion saved = votacionRepository.save(votacion);

            // Registrar actividad
            actividadService.registrarActividad(
                    userId,
                    ActividadTipo.VALORACION.name(),
                    anime.getId(),
                    "ANIME",
                    Map.of("puntuacion", puntuacionInterna)
            );

            return saved.getId() != null;

        } catch (Exception e) {
            System.err.println("Error creando votación: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Encuentra una votación por userId y jikanId
     */
    public Optional<Votacion> findByUserIdAndJikanId(Long userId, Long jikanId) {
        try {
            // Convertir Long a Integer para compatibilidad con el repository
            return votacionRepository.findByUsuario_IdAndAnime_JikanId(userId, jikanId.intValue());
        } catch (Exception e) {
            System.err.println("Error buscando votación: " + e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Actualiza una votación existente por jikanId
     */
    public boolean updateVotacion(Long userId, Long jikanId, Integer puntuacion) {
        try {
            Optional<Votacion> votacionOpt = findByUserIdAndJikanId(userId, jikanId);

            if (votacionOpt.isEmpty()) {
                System.err.println("No se encontró votación para actualizar");
                return false;
            }

            Votacion votacion = votacionOpt.get();
            Double puntuacionAnterior = votacion.getPuntuacion();

            // Convertir puntuación de escala 1-10 a 1-5
            Double puntuacionInterna = puntuacion / 2.0;
            votacion.setPuntuacion(puntuacionInterna);

            votacionRepository.save(votacion);

            // Registrar actividad de actualización
            actividadService.registrarActividad(
                    userId,
                    ActividadTipo.VALORACION.name(),
                    votacion.getAnime().getId(),
                    "ANIME",
                    Map.of(
                            "puntuacionAnterior", puntuacionAnterior,
                            "puntuacionNueva", puntuacionInterna
                    )
            );

            return true;

        } catch (Exception e) {
            System.err.println("Error actualizando votación: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Elimina una votación por userId y jikanId
     */
    public boolean removeVotacionByJikanId(Long userId, Long jikanId) {
        try {
            Optional<Votacion> votacionOpt = findByUserIdAndJikanId(userId, jikanId);

            if (votacionOpt.isEmpty()) {
                System.err.println("No se encontró votación para eliminar");
                return false;
            }

            Votacion votacion = votacionOpt.get();
            votacionRepository.delete(votacion);

            // Registrar actividad de eliminación
            actividadService.registrarActividad(
                    userId,
                    ActividadTipo.VALORACION.name(),
                    votacion.getAnime().getId(),
                    "ANIME",
                    Map.of("accion", "eliminada", "puntuacionEliminada", votacion.getPuntuacion())
            );

            return true;

        } catch (Exception e) {
            System.err.println("Error eliminando votación: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Verifica si existe una votación por userId y jikanId
     */
    public boolean existsVotacionByUserIdAndJikanId(Long userId, Long jikanId) {
        try {
            Optional<Votacion> votacion = findByUserIdAndJikanId(userId, jikanId);
            return votacion.isPresent();
        } catch (Exception e) {
            System.err.println("Error verificando votación: " + e.getMessage());
            return false;
        }
    }

    /**
     * Obtiene estadísticas de un anime por jikanId
     */
    public Map<String, Object> getAnimeStatsByJikanId(Long jikanId) {
        Map<String, Object> stats = new HashMap<>();

        try {
            // Encontrar anime
            Optional<Anime> animeOpt = animeRepository.findByJikanId(jikanId);            if (animeOpt.isEmpty()) {
                stats.put("error", "Anime no encontrado");
                return stats;
            }

            Anime anime = animeOpt.get();
            List<Votacion> votaciones = votacionRepository.findByAnime(anime);

            // Calcular estadísticas
            long totalVotaciones = votaciones.size();
            double promedioVotaciones = 0.0;

            if (totalVotaciones > 0) {
                double suma = votaciones.stream()
                        .mapToDouble(Votacion::getPuntuacion)
                        .sum();
                promedioVotaciones = Math.round((suma / totalVotaciones) * 10.0) / 10.0;
            }

            // Distribución de puntuaciones (convertir a escala 1-10)
            Map<String, Long> distribucion = new HashMap<>();
            for (int i = 1; i <= 10; i++) {
                final int puntuacion = i;
                long count = votaciones.stream()
                        .mapToLong(v -> {
                            int puntuacionExterna = (int) Math.round(v.getPuntuacion() * 2);
                            return puntuacionExterna == puntuacion ? 1 : 0;
                        })
                        .sum();
                distribucion.put(i + " estrella" + (i > 1 ? "s" : ""), count);
            }

            stats.put("totalVotaciones", totalVotaciones);
            stats.put("promedioVotaciones", promedioVotaciones * 2); // Convertir a escala 1-10
            stats.put("distribucionPuntuaciones", distribucion);
            stats.put("animeId", anime.getId());
            stats.put("animeTitulo", anime.getTitulo());
            stats.put("jikanId", jikanId);

        } catch (Exception e) {
            System.err.println("Error obteniendo estadísticas: " + e.getMessage());
            e.printStackTrace();
            stats.put("error", "Error interno del servidor");
        }

        return stats;
    }
}
