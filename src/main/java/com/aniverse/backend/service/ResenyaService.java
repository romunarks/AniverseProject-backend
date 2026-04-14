package com.aniverse.backend.service;

import com.aniverse.backend.dto.*;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Resenya;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.repository.AnimeRepository;
import com.aniverse.backend.repository.FavoritoRepository;
import com.aniverse.backend.repository.ResenyaRepository;
import com.aniverse.backend.repository.UsuarioRepository;
import com.aniverse.backend.util.ActividadTipo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class ResenyaService {

    private static final Logger log = LoggerFactory.getLogger(ResenyaService.class);

    private final ResenyaRepository resenyaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AnimeRepository animeRepository;
    private final FavoritoRepository favoritoRepository; // ✅ AÑADIDO para validación
    private final ExternalAnimeService externalAnimeService;
    private final ActividadService actividadService;

    public ResenyaService(ResenyaRepository resenyaRepository,
                          UsuarioRepository usuarioRepository,
                          AnimeRepository animeRepository,
                          FavoritoRepository favoritoRepository, // ✅ AÑADIDO
                          ExternalAnimeService externalAnimeService,
                          ActividadService actividadService) {
        this.resenyaRepository = resenyaRepository;
        this.usuarioRepository = usuarioRepository;
        this.animeRepository = animeRepository;
        this.favoritoRepository = favoritoRepository; // ✅ AÑADIDO
        this.externalAnimeService = externalAnimeService;
        this.actividadService = actividadService;
    }

    // ===============================================
    // MÉTODOS PRINCIPALES DE RESEÑAS
    // ===============================================

    /**
     * Crea una nueva reseña - SOLO PARA ANIMES FAVORITOS
     */
    @Transactional
    public ResenyaDTO crearResenya(Long usuarioId, ResenyaCrearDTO resenyaDTO) {
        log.info("Creando reseña - Usuario: {}, AnimeId: {}, JikanId: {}",
                usuarioId, resenyaDTO.getAnimeId(), resenyaDTO.getJikanId());

        // Validar usuario
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        // Buscar el anime
        Anime anime = buscarAnimeParaResenya(resenyaDTO.getAnimeId(), resenyaDTO.getJikanId());

        // ✅ VALIDACIÓN CLAVE: Verificar que el anime esté en favoritos del usuario
        if (!favoritoRepository.existsByUsuarioAndAnime(usuario, anime)) {
            throw new IllegalArgumentException(
                    "Solo puedes reseñar animes que estén en tus favoritos. " +
                            "Agrega '" + anime.getTitulo() + "' a favoritos primero."
            );
        }

        // Verificar que no haya reseñado ya este anime
        if (resenyaRepository.existsByUsuarioAndAnimeAndEliminadoFalse(usuario, anime)) {
            throw new IllegalArgumentException("Ya has reseñado este anime");
        }

        // Crear la reseña
        Resenya resenya = new Resenya(usuario, anime, resenyaDTO.getContenido(), resenyaDTO.getPuntuacion());
        Resenya savedResenya = resenyaRepository.save(resenya);

        // Registrar actividad
        try {
            actividadService.registrarActividad(
                    usuarioId,
                    ActividadTipo.RESENYA_CREATE.name(),
                    anime.getId(),
                    "ANIME",
                    null
            );
        } catch (Exception e) {
            log.warn("Error registrando actividad de reseña: {}", e.getMessage());
        }

        log.info("Reseña creada exitosamente - ID: {}, Usuario: {}, Anime: {}",
                savedResenya.getId(), usuario.getNombre(), anime.getTitulo());

        return convertToDTO(savedResenya);
    }

    /**
     * Actualiza una reseña existente
     */
    @Transactional
    public ResenyaDTO actualizarResenya(Long resenyaId, Long usuarioId, ResenyaActualizarDTO updateDTO) {
        log.info("Actualizando reseña - ID: {}, Usuario: {}", resenyaId, usuarioId);

        Resenya resenya = resenyaRepository.findByIdAndEliminadoFalse(resenyaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada con ID: " + resenyaId));

        // Verificar que el usuario es el autor de la reseña
        if (!resenya.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Solo puedes actualizar tus propias reseñas");
        }

        // Actualizar campos
        if (updateDTO.getContenido() != null) {
            resenya.setContenido(updateDTO.getContenido());
        }
        if (updateDTO.getPuntuacion() != null) {
            resenya.setPuntuacion(updateDTO.getPuntuacion());
        }

        Resenya updatedResenya = resenyaRepository.save(resenya);

        // Registrar actividad
        try {
            actividadService.registrarActividad(
                    usuarioId,
                    ActividadTipo.RESENYA_UPDATE.name(),
                    resenya.getAnime().getId(),
                    "ANIME",
                    null
            );
        } catch (Exception e) {
            log.warn("Error registrando actividad de actualización: {}", e.getMessage());
        }

        log.info("Reseña actualizada exitosamente - ID: {}", resenyaId);
        return convertToDTO(updatedResenya);
    }

    /**
     * Elimina una reseña (soft delete)
     */
    @Transactional
    public void eliminarResenya(Long resenyaId, Long usuarioId) {
        log.info("Eliminando reseña - ID: {}, Usuario: {}", resenyaId, usuarioId);

        Resenya resenya = resenyaRepository.findByIdAndEliminadoFalse(resenyaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada con ID: " + resenyaId));

        // Verificar que el usuario es el autor de la reseña
        if (!resenya.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Solo puedes eliminar tus propias reseñas");
        }

        // Soft delete
        resenya.marcarComoEliminado();
        resenyaRepository.save(resenya);

        // Registrar actividad
        try {
            actividadService.registrarActividad(
                    usuarioId,
                    ActividadTipo.RESENYA_DELETE.name(),
                    resenya.getAnime().getId(),
                    "ANIME",
                    null
            );
        } catch (Exception e) {
            log.warn("Error registrando actividad de eliminación: {}", e.getMessage());
        }

        log.info("Reseña eliminada exitosamente - ID: {}", resenyaId);
    }

    // ===============================================
    // MÉTODOS DE CONSULTA
    // ===============================================

    /**
     * Obtiene todas las reseñas activas con paginación
     */
    public Page<ResenyaDTO> getAllResenyas(Pageable pageable) {
        return resenyaRepository.findAllActive(pageable)
                .map(this::convertToDTO);
    }

    /**
     * Obtiene reseñas de un anime específico
     */
    // src/main/java/com/aniverse/backend/service/ResenyaService.java

    // src/main/java/com/aniverse/backend/service/ResenyaService.java
    @Transactional(readOnly = true) // ✅ Vital para evitar cortes de conexión
    public Page<ResenyaDTO> getResenyasByAnime(Long animeId, Pageable pageable) {
        log.debug("Obteniendo reseñas paginadas para el anime ID: {}", animeId);

        // 1. Verificar que el anime exista en local
        if (!animeRepository.existsById(animeId)) {
            throw new ResourceNotFoundException("Anime no encontrado con ID: " + animeId);
        }

        // 2. Usar el nuevo método del repositorio que trae al usuario integrado (JOIN FETCH)
        return resenyaRepository.findByAnimeIdActiveWithUserPaginated(animeId, pageable)
                .map(this::convertToDTO);
    }
    /**
     * Obtiene reseñas de un usuario específico - VERSIÓN SIN LAZY LOADING
     */
    @Transactional(readOnly = true)
    public Page<ResenyaDTO> getResenyasByUsuario(Long usuarioId, Pageable pageable) {
        log.debug("Obteniendo reseñas para usuario: {}", usuarioId);

        // Verificar que el usuario existe
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + usuarioId));

        // ✅ USAR EL MÉTODO CON FETCH JOIN PARA EVITAR LAZY LOADING
        List<Resenya> todasLasResenyas = resenyaRepository.findByUsuarioIdWithDetails(usuarioId);

        // Aplicar paginación manualmente
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), todasLasResenyas.size());

        List<Resenya> resenyasPaginadas = todasLasResenyas.subList(start, end);

        // Convertir a DTOs
        List<ResenyaDTO> resenyasDTO = resenyasPaginadas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());

        // Crear Page con los resultados
        return new PageImpl<>(resenyasDTO, pageable, todasLasResenyas.size());
    }

    /**
     * MÉTODO ADICIONAL: Obtener reseñas de usuario como lista simple (sin paginación)
     */
    @Transactional(readOnly = true)
    public List<ResenyaDTO> getResenyasByUsuarioId(Long usuarioId) {
        log.debug("Obteniendo todas las reseñas para usuario: {}", usuarioId);

        // ✅ USAR EL MÉTODO CON FETCH JOIN
        List<Resenya> resenyas = resenyaRepository.findByUsuarioIdWithDetails(usuarioId);

        return resenyas.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    /**
     * Obtiene estadísticas de reseñas de un anime
     */
    public Map<String, Object> getEstadisticasAnime(Long animeId) {
        Anime anime = animeRepository.findByIdAndEliminadoFalse(animeId)
                .orElseThrow(() -> new ResourceNotFoundException("Anime no encontrado con ID: " + animeId));

        List<Resenya> resenyas = resenyaRepository.findByAnimeAndEliminadoFalse(anime);

        Map<String, Object> estadisticas = new HashMap<>();
        estadisticas.put("totalResenyas", resenyas.size());

        if (!resenyas.isEmpty()) {
            double promedio = resenyas.stream()
                    .mapToDouble(Resenya::getPuntuacion)
                    .average()
                    .orElse(0.0);

            estadisticas.put("puntuacionPromedio", Math.round(promedio * 100.0) / 100.0);
            estadisticas.put("puntuacionMaxima", resenyas.stream()
                    .mapToDouble(Resenya::getPuntuacion)
                    .max().orElse(0.0));
            estadisticas.put("puntuacionMinima", resenyas.stream()
                    .mapToDouble(Resenya::getPuntuacion)
                    .min().orElse(0.0));
        } else {
            estadisticas.put("puntuacionPromedio", 0.0);
            estadisticas.put("puntuacionMaxima", 0.0);
            estadisticas.put("puntuacionMinima", 0.0);
        }

        return estadisticas;
    }

    // ===============================================
    // MÉTODOS DE VALIDACIÓN PARA FAVORITOS
    // ===============================================

    /**
     * Verifica si un usuario ya reseñó un anime
     */
    public boolean usuarioYaResenoAnime(Long usuarioId, Long animeId, Long jikanId) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Anime anime = buscarAnimeParaResenya(animeId, jikanId);

        return resenyaRepository.existsByUsuarioAndAnimeAndEliminadoFalse(usuario, anime);
    }

    /**
     * Obtiene la lista de animes favoritos del usuario que puede reseñar
     */
    public List<Anime> getAnimesFavoritosParaResenar(Long usuarioId) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        // ✅ USAR MÉTODO QUE EXISTE EN TU REPOSITORY
        return favoritoRepository.findAnimesFavoritosByUsuario(usuarioId);
    }

    /**
     * Verifica si un usuario puede reseñar un anime específico
     */
    public boolean puedeResenarAnime(Long usuarioId, Long animeId, Long jikanId) {
        try {
            Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                    .orElse(null);
            if (usuario == null) return false;

            Anime anime = buscarAnimeParaResenya(animeId, jikanId);
            if (anime == null) return false;

            // Verificar que esté en favoritos
            if (!favoritoRepository.existsByUsuarioAndAnime(usuario, anime)) {
                return false;
            }

            // Verificar que no haya reseñado ya
            return !resenyaRepository.existsByUsuarioAndAnimeAndEliminadoFalse(usuario, anime);

        } catch (Exception e) {
            log.error("Error verificando si puede reseñar: {}", e.getMessage());
            return false;
        }
    }

    // ===============================================
    // MÉTODOS ADMINISTRATIVOS
    // ===============================================

    /**
     * Obtiene reseñas eliminadas (para administradores)
     */
    public Page<ResenyaDTO> getDeletedResenyas(Pageable pageable) {
        return resenyaRepository.findAllDeleted(pageable)
                .map(this::convertToDTO);
    }

    /**
     * Restaura una reseña eliminada (para administradores)
     */
    @Transactional
    public Resenya restoreResenya(Long resenyaId) {
        Resenya resenya = resenyaRepository.findById(resenyaId)
                .orElseThrow(() -> new ResourceNotFoundException("Reseña no encontrada con ID: " + resenyaId));

        if (!resenya.isEliminado()) {
            throw new IllegalStateException("La reseña no está eliminada");
        }

        resenya.restaurar();
        return resenyaRepository.save(resenya);
    }

    // ===============================================
    // MÉTODOS DE UTILIDAD Y CONVERSIÓN
    // ===============================================

    /**
     * Busca un anime para reseña (por ID local o JikanId)
     */
    private Anime buscarAnimeParaResenya(Long animeId, Long jikanId) {
        if (animeId != null) {
            return animeRepository.findByIdAndEliminadoFalse(animeId)
                    .orElseThrow(() -> new ResourceNotFoundException("Anime no encontrado con ID: " + animeId));
        } else if (jikanId != null) {
            // Buscar por JikanId en la BD local primero
            return animeRepository.findByJikanIdAndEliminadoFalse(jikanId)
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Anime no encontrado con JikanId: " + jikanId +
                                    ". Debe estar en favoritos para poder reseñarlo."));
        } else {
            throw new IllegalArgumentException("Se requiere animeId o jikanId");
        }
    }

    /**
     * Convierte entidad Resenya a DTO
     */
    public ResenyaDTO convertToDTO(Resenya resenya) {
        return new ResenyaDTO(
                resenya.getId(),
                resenya.getUsuario().getId(),
                resenya.getUsuario().getNombre(),
                resenya.getAnime().getId(),
                resenya.getAnime().getTitulo(),
                resenya.getContenido(),
                resenya.getPuntuacion(),
                resenya.getFechaCreacion(),
                resenya.getFechaActualizacion()
        );
    }

    // ===============================================
    // MÉTODOS LEGACY (COMPATIBILIDAD)
    // ===============================================

    /**
     * Método legacy para compatibilidad con código existente
     */
    @Transactional
    public Resenya saveResenya(Resenya resenya) {
        // Validar que el anime esté en favoritos
        if (!favoritoRepository.existsByUsuarioAndAnime(resenya.getUsuario(), resenya.getAnime())) {
            throw new IllegalArgumentException(
                    "Solo puedes reseñar animes que estén en tus favoritos. " +
                            "Agrega '" + resenya.getAnime().getTitulo() + "' a favoritos primero."
            );
        }

        // Validar que no haya reseñado ya
        if (resenyaRepository.existsByUsuarioAndAnimeAndEliminadoFalse(resenya.getUsuario(), resenya.getAnime())) {
            throw new IllegalArgumentException("Ya has reseñado este anime");
        }

        return resenyaRepository.save(resenya);
    }

    /**
     * Método legacy para soft delete
     */
    @Transactional
    public void softDeleteResenya(Long id) {
        Resenya resenya = resenyaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Reseña no encontrada con ID: " + id));

        resenya.marcarComoEliminado();
        resenyaRepository.save(resenya);
    }
}