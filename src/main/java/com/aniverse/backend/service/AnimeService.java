package com.aniverse.backend.service;

import com.aniverse.backend.dto.AnimeDTO;
import com.aniverse.backend.dto.AnimeDetalleDTO;
import com.aniverse.backend.exception.DuplicateResourceException;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Resenya;
import com.aniverse.backend.model.Votacion;
import com.aniverse.backend.repository.AnimeRepository;
import com.aniverse.backend.repository.ResenyaRepository;
import com.aniverse.backend.repository.VotacionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AnimeService {

    private static final Logger log = LoggerFactory.getLogger(AnimeService.class);

    private final AnimeRepository animeRepository;
    private final ImagenService imagenService;
    private final ResenyaRepository resenyaRepository;
    private final VotacionRepository votacionRepository;
    private final ExternalAnimeService externalAnimeService;

    public AnimeService(AnimeRepository animeRepository,
                        ImagenService imagenService,
                        ResenyaRepository resenyaRepository,
                        VotacionRepository votacionRepository,
                        ExternalAnimeService externalAnimeService) {
        this.animeRepository = animeRepository;
        this.imagenService = imagenService;
        this.resenyaRepository = resenyaRepository;
        this.votacionRepository = votacionRepository;
        this.externalAnimeService = externalAnimeService;
    }

    // AnimeService.java

    @Cacheable(value = "animeListCache",
            key = "'page_' + (#pageable != null ? #pageable.pageNumber : 0) + '_size_' + (#pageable != null ? #pageable.pageSize : 10)",
            unless = "#result == null || #result.isEmpty()")
    public Page<AnimeDTO> getAllAnimes(Pageable pageable) {
        // Asegurar que pageable nunca sea null
        if (pageable == null) {
            pageable = PageRequest.of(0, 10);
        }

        return animeRepository.findAllActive(pageable)
                .map(this::convertToDTO);
    }

    // Método helper para convertir entidad a DTO
    public AnimeDTO convertToDTO(Anime anime) {
        AnimeDTO dto = new AnimeDTO(
                anime.getId(),
                anime.getTitulo(),
                anime.getDescripcion(),
                anime.getGenero(),
                anime.getImagenUrl()
        );
        dto.setJikanId(anime.getJikanId());
        return dto;
    }

    @Cacheable(value = "animeDetalleCache", key = "#id")
    public AnimeDetalleDTO getAnimeDetalle(Long id) {
        Anime anime = animeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anime", id));

        // Cargar reseñas y votaciones de manera separada si es necesario
        List<Resenya> resenyas = resenyaRepository.findByAnime(anime);
        List<Votacion> votaciones = votacionRepository.findByAnime(anime);

        AnimeDetalleDTO detalleDTO = new AnimeDetalleDTO(
                anime.getId(),
                anime.getTitulo(),
                anime.getDescripcion(),
                anime.getGenero(),
                anime.getImagenUrl(),
                anime.getCreatedAt(),
                anime.getUpdatedAt(),
                anime.getCreatedBy(),
                anime.getUpdatedBy()
        );

        // Establecer jikanId
        detalleDTO.setJikanId(anime.getJikanId());

        // Si necesitas añadir reseñas o votaciones al DTO
        return detalleDTO;
    }

    /**
     * Obtiene un anime por su ID (local)
     *
     * @param id ID local del anime
     * @return El anime encontrado
     * @throws ResourceNotFoundException si no se encuentra el anime
     */

    /**
     * Obtiene un anime por su ID (local)
     *
     * @param id ID local del anime
     * @return El anime encontrado
     * @throws ResourceNotFoundException si no se encuentra el anime
     *  @throws IllegalArgumentException si el ID no es válido
     */
    @Cacheable(value = "animeCache", key = "#id")
    public Anime getAnimeById(Long id) {
        log.debug("Buscando anime por ID local: {}", id);
        // Buscar en la base de datos local por el ID proporcionado.
        // Asegúrate de que el ID sea positivo si tus IDs locales siempre lo son.
        // Si se espera que los IDs locales puedan ser negativos por alguna razón MUY específica,
        // entonces esta comprobación no es necesaria, pero es inusual.
        if (id == null || id <= 0) { // O solo id == null si los IDs pueden ser 0 o negativos
            log.warn("Se intentó buscar un anime con ID local inválido: {}", id);
            throw new IllegalArgumentException("El ID del anime local debe ser un número positivo.");
        }

        return animeRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> {
                    log.warn("Anime no encontrado con ID local {}: {}", id);
                    return new ResourceNotFoundException("Anime no encontrado con ID local: " + id);
                });
    }



    /**
     * Obtiene un anime por su ID de Jikan (MAL ID)
     * Si no existe localmente, lo busca en la API de Jikan y lo guarda
     *
     * @param jikanId ID de Jikan (MAL ID)
     * @return El anime encontrado o creado
     * @throws ResourceNotFoundException si no se encuentra el anime ni en local ni en Jikan
     */
    @Transactional
    @Cacheable(value = "animeCache", key = "'jikan_' + #jikanId")
    public Anime getAnimeByJikanId(Long jikanId) {
        // Primero buscar en la base de datos local
        Optional<Anime> localAnime = animeRepository.findByJikanIdAndEliminadoFalse(jikanId);

        if (localAnime.isPresent()) {
            log.debug("Anime con jikanId {} encontrado localmente", jikanId);
            return localAnime.get();
        }

        // Si no está en local, usar el servicio externo para buscarlo y guardarlo
        log.info("Anime con jikanId {} no encontrado localmente, buscando en API externa", jikanId);
        return externalAnimeService.findOrSaveExternalAnime(jikanId);
    }



    // Soft delete (eliminación lógica)
    @Transactional
    @CacheEvict(value = {"animeCache", "animeDetalleCache"}, key = "#id")
    public void softDeleteAnime(Long id) {
        Anime anime = animeRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anime", id));

        anime.setEliminado(true);
        anime.setFechaEliminacion(LocalDateTime.now());

        animeRepository.save(anime);
        log.info("Anime con ID {} marcado como eliminado", id);
    }

    // Guardar un nuevo anime
    @Transactional
    @CacheEvict(value = {"animeCache", "animeDetalleCache", "animeListCache"}, allEntries = true)
    public Anime saveAnime(Anime anime) {
        if (anime.getId() != null && animeRepository.existsById(anime.getId())) {
            throw new DuplicateResourceException("Ya existe un anime con este ID: " + anime.getId());
        }

        // Verificar que el jikanId no esté duplicado si se proporciona
        if (anime.getJikanId() != null && animeRepository.findByJikanId(anime.getJikanId()).isPresent()) {
            throw new DuplicateResourceException("Ya existe un anime con este JikanID: " + anime.getJikanId());
        }

        if (anime.getTitulo() == null || anime.getTitulo().isEmpty()) {
            throw new IllegalArgumentException("El título del anime no puede estar vacío.");
        }

        anime.setEliminado(false);
        Anime savedAnime = animeRepository.save(anime);
        log.info("Anime guardado con ID {}: {}", savedAnime.getId(), savedAnime.getTitulo());
        return savedAnime;
    }

    // Actualizar un anime existente
    @Transactional
    @CacheEvict(value = {"animeCache", "animeDetalleCache"}, key = "#id")
    public Anime updateAnime(Long id, Anime animeActualizado) {
        Anime animeExistente = animeRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anime", id));

        // Verificar que el jikanId no esté duplicado si se cambia
        if (animeActualizado.getJikanId() != null &&
                !animeActualizado.getJikanId().equals(animeExistente.getJikanId()) &&
                animeRepository.findByJikanId(animeActualizado.getJikanId()).isPresent()) {
            throw new DuplicateResourceException("Ya existe un anime con este JikanID: " + animeActualizado.getJikanId());
        }

        // Actualizar solo los campos proporcionados
        if (animeActualizado.getTitulo() != null) {
            animeExistente.setTitulo(animeActualizado.getTitulo());
        }
        if (animeActualizado.getDescripcion() != null) {
            animeExistente.setDescripcion(animeActualizado.getDescripcion());
        }
        if (animeActualizado.getGenero() != null) {
            animeExistente.setGenero(animeActualizado.getGenero());
        }
        if (animeActualizado.getAnyo() != 0) {
            animeExistente.setAnyo(animeActualizado.getAnyo());
        }
        if (animeActualizado.getTemporada() != null) {
            animeExistente.setTemporada(animeActualizado.getTemporada());
        }
        if (animeActualizado.getImagenUrl() != null) {
            animeExistente.setImagenUrl(animeActualizado.getImagenUrl());
        }
        if (animeActualizado.getJikanId() != null) {
            animeExistente.setJikanId(animeActualizado.getJikanId());
        }

        Anime updated = animeRepository.save(animeExistente);
        log.info("Anime con ID {} actualizado: {}", id, updated.getTitulo());
        return updated;
    }

    // Actualizar buscarAnimes para incluir imagenUrl y jikanId
    // Buscar animes con filtros
    public Page<AnimeDTO> buscarAnimes(String titulo, String genero, Integer anyo, Pageable pageable) {
        return animeRepository.findByFiltersActive(titulo, genero, anyo, pageable)
                .map(anime -> {
                    AnimeDTO dto = new AnimeDTO(
                            anime.getId(),
                            anime.getTitulo(),
                            anime.getDescripcion(),
                            anime.getGenero(),
                            anime.getImagenUrl()
                    );
                    dto.setJikanId(anime.getJikanId());
                    return dto;
                });
    }

    // Obtener elementos eliminados (para administradores)
    public Page<AnimeDTO> getDeletedAnimes(Pageable pageable) {
        return (Page<AnimeDTO>) animeRepository.findAll(pageable)
                .filter(Anime::isEliminado)
                .map(anime -> {
                    AnimeDTO dto = new AnimeDTO(
                            anime.getId(),
                            anime.getTitulo(),
                            anime.getDescripcion(),
                            anime.getGenero(),
                            anime.getImagenUrl()
                    );
                    dto.setJikanId(anime.getJikanId());
                    return dto;
                });
    }

    // Restaurar un anime eliminado
    @Transactional
    @CacheEvict(value = {"animeCache", "animeDetalleCache"}, key = "#id")
    public Anime restoreAnime(Long id) {
        Anime anime = animeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anime", id));

        if (!anime.isEliminado()) {
            throw new IllegalStateException("El anime no está eliminado, no se puede restaurar");
        }

        anime.setEliminado(false);
        anime.setFechaEliminacion(null);

        Anime restored = animeRepository.save(anime);
        log.info("Anime con ID {} restaurado: {}", id, restored.getTitulo());
        return restored;
    }
    /**
     * Método unificado para obtener un anime, ya sea por ID local o por ID de Jikan
     *
     * @param id ID que puede ser local o de Jikan
     * @param isJikanId indica si el ID proporcionado es un ID de Jikan
     * @return AnimeDTO con la información del anime
     * @throws ResourceNotFoundException si no se encuentra el anime
     */
    @Transactional
    public AnimeDTO getAnimeUnificado(Long id, boolean isJikanId) {
        log.debug("Buscando anime con {}ID: {}", isJikanId ? "Jikan" : "local", id);

        if (id == null) {
            throw new IllegalArgumentException("El ID no puede ser nulo");
        }

        Anime anime = null;

        try {
            if (isJikanId) {
                // Buscar por ID de Jikan en la base de datos local primero
                Optional<Anime> localAnime = animeRepository.findByJikanIdAndEliminadoFalse(id);

                if (localAnime.isPresent()) {
                    anime = localAnime.get();
                    log.debug("Anime con jikanId {} encontrado localmente: {}", id, anime.getTitulo());
                } else {
                    // Si no está en la base local, intentar buscarlo y guardarlo desde la API externa
                    log.info("Anime con jikanId {} no encontrado localmente, buscando en API externa", id);
                    anime = externalAnimeService.findOrSaveExternalAnime(id);

                    if (anime == null) {
                        throw new ResourceNotFoundException("No se encontró el anime con JikanID: " + id);
                    }
                }
            } else {
                // Buscar por ID local
                Optional<Anime> localAnime = animeRepository.findByIdAndEliminadoFalse(id);

                if (localAnime.isPresent()) {
                    anime = localAnime.get();
                    log.debug("Anime con ID local {} encontrado: {}", id, anime.getTitulo());
                } else {
                    throw new ResourceNotFoundException("No se encontró el anime con ID local: " + id);
                }
            }
        } catch (ResourceNotFoundException e) {
            log.warn("No se encontró el anime con {}ID: {}", isJikanId ? "Jikan" : "local", id);
            throw e;
        } catch (Exception e) {
            log.error("Error al buscar anime con {}ID {}: {}", isJikanId ? "Jikan" : "local", id, e.getMessage());
            throw new RuntimeException("Error al buscar el anime: " + e.getMessage(), e);
        }

        // Si llegamos aquí, el anime se encontró
        // Calcular puntuación promedio
        Double puntuacionPromedio = null;
        if (anime.getVotaciones() != null && !anime.getVotaciones().isEmpty()) {
            puntuacionPromedio = anime.getVotaciones().stream()
                    .mapToDouble(v -> v.getPuntuacion())
                    .average()
                    .orElse(0.0);
        }

        // Crear y retornar el DTO completo
        AnimeDTO dto = new AnimeDTO(
                anime.getId(),
                anime.getJikanId(),
                anime.getTitulo(),
                anime.getDescripcion(),
                anime.getGenero(),
                anime.getImagenUrl(),
                puntuacionPromedio,
                anime.getAnyo(),
                anime.getTemporada()
        );

        log.debug("Anime encontrado: {}", anime.getTitulo());
        return dto;
    }

    /**
     * Sobrecarga del método getAnimeUnificado para manejar IDs como String
     * Identifica automáticamente si es un ID de Jikan basado en un prefijo (por ejemplo, "jikan_123")
     *
     * @param idStr ID en formato String, puede incluir prefijo
     * @return AnimeDTO con la información del anime
     * @throws ResourceNotFoundException si no se encuentra el anime
     * @throws IllegalArgumentException si el formato del ID no es válido
     */
    @Transactional
    public AnimeDTO getAnimeUnificado(String idStr) {
        log.debug("Analizando ID en formato string: {}", idStr);

        if (idStr == null || idStr.trim().isEmpty()) {
            throw new IllegalArgumentException("El ID no puede ser nulo o vacío");
        }

        // Si el ID tiene el formato "jikan_123", extraer el número y buscar por JikanID
        if (idStr.toLowerCase().startsWith("jikan_")) {
            try {
                Long jikanId = Long.parseLong(idStr.substring(6)); // Quitar "jikan_" y convertir a Long
                return getAnimeUnificado(jikanId, true);
            } catch (NumberFormatException e) {
                log.error("Formato de JikanID inválido: {}", idStr);
                throw new IllegalArgumentException("Formato de JikanID inválido: " + idStr);
            }
        }
        // Sino, intentar convertir directamente a Long y buscar por ID local
        else {
            try {
                Long id = Long.parseLong(idStr);
                return getAnimeUnificado(id, false);
            } catch (NumberFormatException e) {
                log.error("Formato de ID inválido: {}", idStr);
                throw new IllegalArgumentException("ID de anime inválido: " + idStr);
            }
        }
    }

    // Eliminación física (solo para casos especiales)
    @Transactional
    @CacheEvict(value = {"animeCache", "animeDetalleCache"}, key = "#id")
    public void hardDeleteAnime(Long id) {
        Anime anime = animeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Anime", id));

        // Eliminar la imagen si existe
        try {
            if (anime.getImagenUrl() != null && !anime.getImagenUrl().isEmpty()) {
                imagenService.eliminarImagen(anime.getImagenUrl());
            }
        } catch (IOException e) {
            // Log error pero continuar con la eliminación
            log.error("Error al eliminar la imagen: {}", e.getMessage());
        }

        animeRepository.deleteById(id);
        log.info("Anime con ID {} eliminado físicamente", id);
    }


    // Obtener animes mejor puntuados
    public List<AnimeDTO> getTopRatedAnimes(int limit) {
        return animeRepository.findTopRatedAnimes(PageRequest.of(0, limit))
                .stream()
                .map(anime -> {
                    AnimeDTO dto = new AnimeDTO(
                            anime.getId(),
                            anime.getTitulo(),
                            anime.getDescripcion(),
                            anime.getGenero(),
                            anime.getImagenUrl()
                    );
                    dto.setJikanId(anime.getJikanId());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Obtener animes más recientes
    public List<AnimeDTO> getMostRecentAnimes(int limit) {
        return animeRepository.findMostRecentAnimes(PageRequest.of(0, limit))
                .stream()
                .map(anime -> {
                    AnimeDTO dto = new AnimeDTO(
                            anime.getId(),
                            anime.getTitulo(),
                            anime.getDescripcion(),
                            anime.getGenero(),
                            anime.getImagenUrl()
                    );
                    dto.setJikanId(anime.getJikanId());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Obtener animes similares por género
    public List<AnimeDTO> getAnimesSimilares(Long animeId, int limit) {
        Anime anime = animeRepository.findByIdAndEliminadoFalse(animeId)
                .orElseThrow(() -> new ResourceNotFoundException("Anime", animeId));

        String genero = anime.getGenero();

        return animeRepository.getAnimesSimilares(animeId, genero, PageRequest.of(0, limit))
                .stream()
                .map(animeSimilar -> {
                    AnimeDTO dto = new AnimeDTO(
                            animeSimilar.getId(),
                            animeSimilar.getTitulo(),
                            animeSimilar.getDescripcion(),
                            animeSimilar.getGenero(),
                            animeSimilar.getImagenUrl()
                    );
                    dto.setJikanId(animeSimilar.getJikanId());
                    return dto;
                })
                .collect(Collectors.toList());
    }

}