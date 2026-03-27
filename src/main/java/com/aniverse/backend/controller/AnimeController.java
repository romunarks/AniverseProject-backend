package com.aniverse.backend.controller;

import com.aniverse.backend.dto.*;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Votacion;
import com.aniverse.backend.repository.AnimeRepository;
import com.aniverse.backend.repository.VotacionRepository;
import com.aniverse.backend.service.AnimeService;
import com.aniverse.backend.service.ExternalAnimeService;
import com.aniverse.backend.service.ImagenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/animes")
@Tag(name = "Anime", description = "API para gestionar animes")
public class AnimeController {

    private static final Logger log = LoggerFactory.getLogger(AnimeController.class);

    private final AnimeService animeService;
    private final ImagenService imagenService;
    private final AnimeRepository animeRepository;
    private final ExternalAnimeService externalAnimeService;
    private final VotacionRepository votacionRepository;

    public AnimeController(AnimeService animeService, ImagenService imagenService,
                           AnimeRepository animeRepository, ExternalAnimeService externalAnimeService,
                           VotacionRepository votacionRepository) {
        this.animeService = animeService;
        this.imagenService = imagenService;
        this.animeRepository = animeRepository;
        this.externalAnimeService = externalAnimeService;
        this.votacionRepository = votacionRepository;
    }


    @GetMapping("/home")
    @Operation(summary = "Obtener animes para la página principal",
            description = "Devuelve una mezcla de animes locales y externos populares")
    public ResponseEntity<AniverseResponse<Map<String, List<AnimeResumidoDTO>>>> getHomeAnimes() {
        try {
            Map<String, List<AnimeResumidoDTO>> homeData = new HashMap<>();

            // 1. Animes locales más recientes
            Page<Anime> recentLocal = animeRepository.findAllActive(PageRequest.of(0, 6, Sort.by(Sort.Direction.DESC, "id")));
            List<AnimeResumidoDTO> localAnimes = recentLocal.getContent().stream()
                    .map(this::convertToResumidoDTO)
                    .collect(Collectors.toList());
            homeData.put("local", localAnimes);

            // 2. Animes trending externos
            try {
                List<Anime> trending = externalAnimeService.getAnimesByCategory(
                        ExternalAnimeService.CATEGORY_TRENDING, 10);
                List<AnimeResumidoDTO> trendingDTOs = trending.stream()
                        .map(this::convertToResumidoDTO)
                        .collect(Collectors.toList());
                homeData.put("trending", trendingDTOs);
            } catch (Exception e) {
                log.error("Error obteniendo trending: {}", e.getMessage());
                homeData.put("trending", new ArrayList<>());
            }

            // 3. Animes recientes externos
            try {
                List<Anime> recent = externalAnimeService.getAnimesByCategory(
                        ExternalAnimeService.CATEGORY_RECENT, 10);
                List<AnimeResumidoDTO> recentDTOs = recent.stream()
                        .map(this::convertToResumidoDTO)
                        .collect(Collectors.toList());
                homeData.put("recent", recentDTOs);
            } catch (Exception e) {
                log.error("Error obteniendo recientes: {}", e.getMessage());
                homeData.put("recent", new ArrayList<>());
            }

            // 4. Top rated externos
            try {
                List<Anime> topRated = externalAnimeService.getAnimesByCategory(
                        ExternalAnimeService.CATEGORY_TOP_RATED, 10);
                List<AnimeResumidoDTO> topRatedDTOs = topRated.stream()
                        .map(this::convertToResumidoDTO)
                        .collect(Collectors.toList());
                homeData.put("topRated", topRatedDTOs);
            } catch (Exception e) {
                log.error("Error obteniendo top rated: {}", e.getMessage());
                homeData.put("topRated", new ArrayList<>());
            }

            return ResponseEntity.ok(AniverseResponse.success("Datos del home obtenidos con éxito", homeData));
        } catch (Exception e) {
            log.error("Error general obteniendo datos del home: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener datos del home"));
        }
    }


    @GetMapping("/mal/{malId}")
    @Operation(
            summary = "Obtener un anime por ID de MyAnimeList",
            description = "Retorna un anime según el ID de MyAnimeList proporcionado",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Anime encontrado"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Anime no encontrado"
                    )
            }
    )
    public ResponseEntity<AniverseResponse<AnimeDTO>> getAnimeByMalId(
            @Parameter(description = "ID de MyAnimeList (Jikan)", required = true)
            @PathVariable Long malId) {

        try {
            Anime anime = externalAnimeService.findOrSaveExternalAnime(malId);

            AnimeDTO animeDTO = new AnimeDTO(
                    anime.getId(),
                    anime.getTitulo(),
                    anime.getDescripcion(),
                    anime.getGenero(),
                    anime.getImagenUrl()
            );

            // Agregar el jikanId al DTO
            animeDTO.setJikanId(anime.getJikanId());

            return ResponseEntity.ok(AniverseResponse.success(animeDTO));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener anime: " + e.getMessage()));
        }
    }

    @GetMapping("/{id}/detalle")
    @Operation(
            summary = "Obtener detalles completos de un anime",
            description = "Retorna un anime con información de auditoría según el ID proporcionado",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Anime encontrado",
                            content = @Content(schema = @Schema(implementation = AnimeDetalleDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Anime no encontrado",
                            content = @Content
                    )
            }
    )
    public ResponseEntity<AniverseResponse<AnimeDetalleDTO>> getAnimeDetalle(@PathVariable Long id) {
        try {
            AnimeDetalleDTO animeDTO = animeService.getAnimeDetalle(id);
            return ResponseEntity.ok(AniverseResponse.success("Detalles del anime obtenidos con éxito", animeDTO));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @PostMapping
    @Operation(
            summary = "Crear un nuevo anime",
            description = "Crea un nuevo anime con los datos proporcionados",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Anime creado correctamente",
                            content = @Content(schema = @Schema(implementation = AnimeDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Datos inválidos",
                            content = @Content
                    )
            }
    )
    public ResponseEntity<AniverseResponse<AnimeDTO>> createAnime(
            @Parameter(description = "Datos del anime a crear", required = true)
            @Valid @RequestBody AnimeCreateDTO animeDTO) {

        // Convertir DTO a entidad
        Anime anime = new Anime();
        anime.setTitulo(animeDTO.getTitulo());
        anime.setDescripcion(animeDTO.getDescripcion());
        anime.setGenero(animeDTO.getGenero());
        anime.setAnyo(animeDTO.getAnyo());
        anime.setTemporada(animeDTO.getTemporada());
        anime.setImagenUrl(animeDTO.getImagenUrl());

        // Establecer jikanId si se proporciona
        if (animeDTO.getJikanId() != null) {
            anime.setJikanId(animeDTO.getJikanId());
        }

        Anime nuevoAnime = animeService.saveAnime(anime);

        // Convertir entidad a DTO de respuesta
        AnimeDTO responseDTO = new AnimeDTO(
                nuevoAnime.getId(),
                nuevoAnime.getTitulo(),
                nuevoAnime.getDescripcion(),
                nuevoAnime.getGenero(),
                nuevoAnime.getImagenUrl()
        );

        // Agregar el jikanId al DTO
        responseDTO.setJikanId(nuevoAnime.getJikanId());

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(AniverseResponse.success("Anime creado exitosamente", responseDTO));
    }

    @PostMapping("/{id}/imagen")
    @Operation(
            summary = "Subir imagen para un anime",
            description = "Sube una imagen y la asocia al anime con el ID proporcionado",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Imagen subida correctamente"),
                    @ApiResponse(responseCode = "400", description = "Error al subir la imagen"),
                    @ApiResponse(responseCode = "404", description = "Anime no encontrado")
            }
    )
    public ResponseEntity<AniverseResponse<String>> subirImagenAnime(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) {
        try {
            Anime anime = animeService.getAnimeById(id);

            // Si ya tiene una imagen, intentar eliminarla
            if (anime.getImagenUrl() != null && !anime.getImagenUrl().isEmpty()) {
                try {
                    imagenService.eliminarImagen(anime.getImagenUrl());
                } catch (IOException e) {
                    // Ignorar errores al eliminar la imagen anterior
                }
            }

            // Guardar la nueva imagen
            String imagenUrl = imagenService.guardarImagen(file);

            // Actualizar el anime con la nueva URL
            anime.setImagenUrl(imagenUrl);
            animeService.updateAnime(id, anime);

            return ResponseEntity.ok(AniverseResponse.success("Imagen subida con éxito", imagenUrl));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Anime no encontrado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error("Error al subir la imagen: " + e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar un anime existente",
            description = "Actualiza un anime con los datos proporcionados",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Anime actualizado correctamente",
                            content = @Content(schema = @Schema(implementation = AnimeDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Anime no encontrado",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Datos inválidos",
                            content = @Content
                    )
            }
    )
    public ResponseEntity<AniverseResponse<AnimeDTO>> updateAnime(
            @PathVariable Long id,
            @Valid @RequestBody AnimeUpdateDTO animeDTO) {
        try {
            Anime anime = new Anime();
            anime.setTitulo(animeDTO.getTitulo());
            anime.setDescripcion(animeDTO.getDescripcion());
            anime.setGenero(animeDTO.getGenero());
            if (animeDTO.getAnyo() != null) {
                anime.setAnyo(animeDTO.getAnyo());
            }
            anime.setTemporada(animeDTO.getTemporada());
            anime.setImagenUrl(animeDTO.getImagenUrl());

            // Actualizar jikanId si se proporciona
            if (animeDTO.getJikanId() != null) {
                anime.setJikanId(animeDTO.getJikanId());
            }

            Anime animeActualizado = animeService.updateAnime(id, anime);

            AnimeDTO responseDTO = new AnimeDTO(
                    animeActualizado.getId(),
                    animeActualizado.getTitulo(),
                    animeActualizado.getDescripcion(),
                    animeActualizado.getGenero(),
                    animeActualizado.getImagenUrl()
            );

            // Agregar el jikanId al DTO
            responseDTO.setJikanId(animeActualizado.getJikanId());

            return ResponseEntity.ok(AniverseResponse.success("Anime actualizado con éxito", responseDTO));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar un anime",
            description = "Elimina lógicamente un anime según el ID proporcionado",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Anime eliminado correctamente"),
                    @ApiResponse(responseCode = "404", description = "Anime no encontrado")
            }
    )
    public ResponseEntity<AniverseResponse<Object>> deleteAnime(@PathVariable Long id) {
        // Verificar si el id pasado existe
        if (!animeRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        animeService.softDeleteAnime(id);
        return ResponseEntity.ok(AniverseResponse.success("El anime con ID " + id + " ha sido eliminado."));
    }

    @GetMapping("/buscar")
    @Operation(
            summary = "Buscar animes por criterios",
            description = "Permite buscar animes locales y externos por título, género y/o año",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Búsqueda realizada correctamente",
                            content = @Content(schema = @Schema(implementation = Page.class))
                    )
            }
    )
    public ResponseEntity<AniverseResponse<Page<AnimeDTO>>> buscarAnimes(
            @RequestParam(required = false) String titulo,
            @RequestParam(required = false) String genero,
            @RequestParam(required = false) Integer anyo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "titulo") String sortBy,
            @RequestParam(defaultValue = "asc") String direction,
            @RequestParam(defaultValue = "true") boolean includeExternal) {

        Sort.Direction dir = direction.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        // Buscar localmente
        Page<AnimeDTO> animes = animeService.buscarAnimes(
                titulo,
                genero,
                anyo,
                PageRequest.of(page, size, Sort.by(dir, sortBy))
        );

        // Si se solicita incluir externos y hay pocos resultados locales, buscar también en API externa
        if (includeExternal && titulo != null && animes.getTotalElements() < size) {
            try {
                // Obtener animes de la API externa
                List<Anime> externalAnimes = externalAnimeService.searchAnimes(titulo);

                // Convertir a DTO - MODIFICADO
                List<AnimeDTO> externalAnimeDTOs = externalAnimes.stream()
                        .map(anime -> {
                            // Crear el DTO usando el constructor existente
                            AnimeDTO dto = new AnimeDTO(
                                    anime.getId(),      // Esto será null si el anime es solo de Jikan y aún no está en tu BD
                                    anime.getTitulo(),
                                    anime.getDescripcion(),
                                    anime.getGenero(),
                                    anime.getImagenUrl()
                            );
                            // MUY IMPORTANTE: Establecer el jikanId
                            dto.setJikanId(anime.getJikanId()); // Asumiendo que la entidad Anime tiene getJikanId()
                            return dto;
                        })
                        .toList();

                // Combinar resultados (preservando la paginación)
                List<AnimeDTO> combinedContent = new ArrayList<>(animes.getContent());

                // Añadir solo resultados externos que no estén ya en los resultados locales
                for (AnimeDTO externalAnime : externalAnimeDTOs) {
                    boolean duplicated = false;
                    for (AnimeDTO localAnime : combinedContent) {
                        if (localAnime.getTitulo().equalsIgnoreCase(externalAnime.getTitulo())) {
                            duplicated = true;
                            break;
                        }
                    }

                    if (!duplicated) {
                        combinedContent.add(externalAnime);
                    }
                }

                // Limitar al tamaño de página
                int endIndex = Math.min(combinedContent.size(), size);
                List<AnimeDTO> paginatedResults = combinedContent.subList(0, endIndex);

                // Crear nueva página con los resultados combinados
                Page<AnimeDTO> combinedPage = new PageImpl<>(
                        paginatedResults,
                        PageRequest.of(page, size),
                        combinedContent.size()
                );

                return ResponseEntity.ok(AniverseResponse.success("Búsqueda realizada con éxito", combinedPage));
            } catch (Exception e) {
                // Si falla la búsqueda externa, devolver solo los resultados locales
                return ResponseEntity.ok(AniverseResponse.success("Búsqueda local realizada con éxito", animes));
            }
        }

        return ResponseEntity.ok(AniverseResponse.success("Búsqueda realizada con éxito", animes));
    }
    public ResponseEntity<AniverseResponse<Page<AnimeDTO>>> getDeletedAnimes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "titulo") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction dir = direction.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        Page<AnimeDTO> animes = animeService.getDeletedAnimes(
                PageRequest.of(page, size, Sort.by(dir, sortBy)));

        return ResponseEntity.ok(AniverseResponse.success("Animes eliminados obtenidos con éxito", animes));
    }

    @PostMapping("/{id}/restaurar")
    @Operation(
            summary = "Restaurar un anime eliminado",
            description = "Restaura un anime que ha sido eliminado lógicamente",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Anime restaurado correctamente",
                            content = @Content(schema = @Schema(implementation = AnimeDTO.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Anime no encontrado",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "El anime no está eliminado",
                            content = @Content
                    )
            }
    )
    public ResponseEntity<AniverseResponse<AnimeDTO>> restoreAnime(
            @Parameter(description = "ID del anime a restaurar", required = true)
            @PathVariable Long id) {

        try {
            Anime animeRestaurado = animeService.restoreAnime(id);

            AnimeDTO responseDTO = new AnimeDTO(
                    animeRestaurado.getId(),
                    animeRestaurado.getTitulo(),
                    animeRestaurado.getDescripcion(),
                    animeRestaurado.getGenero(),
                    animeRestaurado.getImagenUrl()
            );

            // Agregar el jikanId al DTO
            responseDTO.setJikanId(animeRestaurado.getJikanId());

            return ResponseEntity.ok(AniverseResponse.success("Anime restaurado con éxito", responseDTO));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/unificado/{id}")
    public ResponseEntity<AniverseResponse<AnimeDTO>> getAnimeUnificado(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean isJikanId) {

        try {
            Anime animeEncontrado = null;

            if (isJikanId) {
                // Si es un JikanID, buscar por ese campo
                try {
                    Long jikanId = Long.parseLong(id);
                    animeEncontrado = animeRepository.findByJikanId(jikanId).orElse(null);

                    // Si no existe en la BD local, obtener de la API externa
                    if (animeEncontrado == null) {
                        animeEncontrado = externalAnimeService.findOrSaveExternalAnime(jikanId);
                    }
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("JikanID debe ser un número válido");
                }
            } else {
                // Buscar por ID local
                try {
                    Long localId = Long.parseLong(id);
                    animeEncontrado = animeRepository.findByIdAndEliminadoFalse(localId)
                            .orElseThrow(() -> new ResourceNotFoundException("Anime", localId));
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("ID debe ser un número válido");
                }
            }

            if (animeEncontrado != null) {
                AnimeDTO animeDTO = convertToDTO(animeEncontrado);
                return ResponseEntity.ok(AniverseResponse.success("Anime encontrado con éxito", animeDTO));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AniverseResponse.error("Anime no encontrado"));
            }
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener anime: " + e.getMessage()));
        }
    }
    // En AnimeController.java
    @GetMapping("/trending")
    @Operation(summary = "Obtener animes trending", description = "Obtiene los animes en tendencia limitados")
    public ResponseEntity<AniverseResponse<List<AnimeResumidoDTO>>> getTrendingAnimes(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("Solicitando animes trending, límite: {}", limit);

            // Obtener directamente de la categoría específica
            List<Anime> animes = externalAnimeService.getAnimesByCategory(
                    ExternalAnimeService.CATEGORY_TRENDING, limit);

            // Convertir a DTOs para la respuesta
            List<AnimeResumidoDTO> animesDTO = animes.stream()
                    .map(this::convertToResumidoDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(AniverseResponse.success(
                    "Animes trending obtenidos con éxito", animesDTO));
        } catch (Exception e) {
            log.error("Error obteniendo animes trending: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener animes trending"));
        }
    }

    @GetMapping("/recent")
    @Operation(summary = "Obtener animes recientes", description = "Obtiene los animes más recientes limitados")
    public ResponseEntity<AniverseResponse<List<AnimeResumidoDTO>>> getRecentAnimes(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("Solicitando animes recientes, límite: {}", limit);

            // Obtener directamente de la categoría específica
            List<Anime> animes = externalAnimeService.getAnimesByCategory(
                    ExternalAnimeService.CATEGORY_RECENT, limit);

            // Convertir a DTOs para la respuesta
            List<AnimeResumidoDTO> animesDTO = animes.stream()
                    .map(this::convertToResumidoDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(AniverseResponse.success(
                    "Animes recientes obtenidos con éxito", animesDTO));
        } catch (Exception e) {
            log.error("Error obteniendo animes recientes: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener animes recientes"));
        }
    }

    @GetMapping("/top-rated")
    @Operation(summary = "Obtener animes mejor puntuados", description = "Obtiene los animes mejor puntuados limitados")
    public ResponseEntity<AniverseResponse<List<AnimeResumidoDTO>>> getTopRatedAnimes(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            log.info("Solicitando animes mejor puntuados, límite: {}", limit);

            // Obtener directamente de la categoría específica
            List<Anime> animes = externalAnimeService.getAnimesByCategory(
                    ExternalAnimeService.CATEGORY_TOP_RATED, limit);

            // Convertir a DTOs para la respuesta
            List<AnimeResumidoDTO> animesDTO = animes.stream()
                    .map(this::convertToResumidoDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(AniverseResponse.success(
                    "Animes mejor puntuados obtenidos con éxito", animesDTO));
        } catch (Exception e) {
            log.error("Error obteniendo animes mejor puntuados: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener animes mejor puntuados"));
        }
    }

    // Método auxiliar para convertir entidad a DTO resumido
    private AnimeResumidoDTO convertToResumidoDTO(Anime anime) {
        AnimeResumidoDTO dto = new AnimeResumidoDTO();
        dto.setId(anime.getId());
        dto.setTitulo(anime.getTitulo());
        dto.setImagenUrl(anime.getImagenUrl());
        dto.setGenero(anime.getGenero());
        dto.setJikanId(anime.getJikanId());

        // Calcular puntuación promedio si tiene votaciones
        if (anime.getId() != null && anime.getVotaciones() != null && !anime.getVotaciones().isEmpty()) {
            double promedio = anime.getVotaciones().stream()
                    .mapToDouble(Votacion::getPuntuacion)
                    .average()
                    .orElse(0.0);
            dto.setPuntuacionPromedio(promedio);
        } else {
            dto.setPuntuacionPromedio(0.0); // Default para animes externos o sin votaciones
        }

        return dto;
    }

    // Este endpoint es para jikanId exclusivamente
    @GetMapping("/external/{jikanId}")
    @Operation(
            summary = "Obtener un anime por su ID de MyAnimeList",
            description = "Retorna un anime según el ID de MyAnimeList proporcionado"
    )
    public ResponseEntity<AniverseResponse<AnimeDTO>> getAnimeByJikanId(
            @PathVariable Long jikanId) {

        try {
            log.debug("Buscando anime por ID de Jikan: {}", jikanId);
            Anime anime = animeService.getAnimeByJikanId(jikanId);

            AnimeDTO animeDTO = new AnimeDTO(
                    anime.getId(),
                    anime.getJikanId(),
                    anime.getTitulo(),
                    anime.getDescripcion(),
                    anime.getGenero(),
                    anime.getImagenUrl()
            );

            return ResponseEntity.ok(AniverseResponse.success("Anime encontrado por Jikan ID", animeDTO));
        } catch (ResourceNotFoundException e) {
            log.warn("No se encontró anime con ID de Jikan {}: {}", jikanId, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            log.warn("Intento de búsqueda con ID de Jikan inválido {}: {}", jikanId, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }


    @GetMapping("/external/featured")
    @Operation(
            summary = "Obtener anime destacado",
            description = "Obtiene un anime destacado desde fuentes externas",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Anime destacado obtenido con éxito",
                            content = @Content(schema = @Schema(implementation = AnimeDTO.class))
                    )
            }
    )
    public ResponseEntity<AniverseResponse<AnimeDTO>> getFeaturedAnime() {
        List<Anime> trending = externalAnimeService.fetchTrendingAnimes();
        if (!trending.isEmpty()) {
            Anime featured = trending.get(0);
            AnimeDTO featuredDTO = new AnimeDTO(
                    featured.getId(),
                    featured.getTitulo(),
                    featured.getDescripcion(),
                    featured.getGenero(),
                    featured.getImagenUrl()
            );
            // Agregar el jikanId al DTO
            featuredDTO.setJikanId(featured.getJikanId());

            return ResponseEntity.ok(AniverseResponse.success("Anime destacado obtenido con éxito", featuredDTO));
        }
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(AniverseResponse.error("No se encontraron animes destacados"));
    }

    @GetMapping("/{animeId}/recomendados")
    @Operation(summary = "Obtener animes recomendados basados en un anime específico")
    public ResponseEntity<AniverseResponse<List<AnimeResumidoDTO>>> getAnimesRecomendados(
            @PathVariable Long animeId,
            @RequestParam(defaultValue = "6") int limit) {

        try {
            // Buscar animes similares por género
            List<AnimeDTO> similares = animeService.getAnimesSimilares(animeId, limit);

            // Convertir a AnimeResumidoDTO
            List<AnimeResumidoDTO> recomendados = similares.stream()
                    .map(anime -> new AnimeResumidoDTO(
                            anime.getId(),
                            anime.getJikanId(),
                            anime.getTitulo(),
                            anime.getImagenUrl(),
                            anime.getGenero(),
                            anime.getPuntuacionPromedio() != null ? anime.getPuntuacionPromedio() : 0.0
                    ))
                    .collect(Collectors.toList());

            return ResponseEntity.ok(AniverseResponse.success(
                    "Animes recomendados obtenidos con éxito", recomendados));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo animes recomendados: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }
    @GetMapping
    public ResponseEntity<AniverseResponse<Page<AnimeDTO>>> getAnimes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        try {
            Sort sort = Sort.by(Sort.Direction.fromString(direction), sortBy);
            Pageable pageable = PageRequest.of(page, size, sort);

            Page<AnimeDTO> animes = animeService.getAllAnimes(pageable);

            return ResponseEntity.ok(
                    AniverseResponse.success("Animes obtenidos con éxito", animes)
            );
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener animes: " + e.getMessage()));
        }
    }

    @GetMapping("/external/search")
    @Operation(
            summary = "Buscar animes en fuentes externas",
            description = "Busca animes en APIs externas por título",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Búsqueda realizada con éxito",
                            content = @Content(schema = @Schema(implementation = AnimeResumidoDTO.class))
                    )
            }
    )
    public ResponseEntity<AniverseResponse<List<AnimeResumidoDTO>>> searchExternalAnimes(
            @Parameter(description = "Término de búsqueda")
            @RequestParam String query) {
        List<Anime> animes = externalAnimeService.searchAnimes(query);
        List<AnimeResumidoDTO> animeDTOs = animes.stream()
                .map(anime -> {
                    AnimeResumidoDTO dto = new AnimeResumidoDTO();
                    dto.setId(anime.getId());
                    dto.setTitulo(anime.getTitulo());
                    dto.setImagenUrl(anime.getImagenUrl());
                    dto.setGenero(anime.getGenero());
                    dto.setPuntuacionPromedio(0.0);
                    // Agregar el jikanId al DTO
                    dto.setJikanId(anime.getJikanId());
                    return dto;
                })
                .collect(Collectors.toList());
        return ResponseEntity.ok(AniverseResponse.success("Búsqueda externa realizada con éxito", animeDTOs));
    }
    /**
     * Método auxiliar para convertir Anime a AnimeDTO
     */
    private AnimeDTO convertToDTO(Anime anime) {
        AnimeDTO dto = new AnimeDTO(
                anime.getId(),
                anime.getTitulo(),
                anime.getDescripcion(),
                anime.getGenero(),
                anime.getImagenUrl()
        );

        // Si tu AnimeDTO tiene un campo jikanId, asegúrate de establecerlo
        if (anime.getJikanId() != null) {
            dto.setJikanId(anime.getJikanId());
        }

        return dto;
    }


    @PostMapping("/save-external")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<AniverseResponse<AnimeDTO>> guardarAnimeExterno(@RequestBody AnimeCreateDTO animeDTO) {
        try {
            // Verificar si ya existe un anime con ese jikanId
            if (animeDTO.getJikanId() != null) {
                Optional<Anime> existente = animeRepository.findByJikanId(animeDTO.getJikanId());
                if (existente.isPresent()) {
                    AnimeDTO existenteDTO = convertToDTO(existente.get());
                    return ResponseEntity.ok(AniverseResponse.success(
                            "El anime ya existe en la biblioteca local", existenteDTO));
                }
            }

            // Crear nueva entidad
            Anime anime = new Anime();
            anime.setTitulo(animeDTO.getTitulo());
            anime.setDescripcion(animeDTO.getDescripcion());
            anime.setGenero(animeDTO.getGenero());
            anime.setAnyo(animeDTO.getAnyo());
            anime.setTemporada(animeDTO.getTemporada());
            anime.setImagenUrl(animeDTO.getImagenUrl());
            anime.setJikanId(animeDTO.getJikanId());

            Anime savedAnime = animeService.saveAnime(anime);
            AnimeDTO savedDTO = convertToDTO(savedAnime);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AniverseResponse.success("Anime externo guardado con éxito", savedDTO));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al guardar anime externo: " + e.getMessage()));
        }
    }
    // En AnimeController.java

// ... (otros imports y la clase)

    // Este endpoint ahora es EXCLUSIVAMENTE para IDs locales (PK de tu base de datos)
    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener un anime por su ID local", // Descripción actualizada
            description = "Retorna un anime según su ID primario en la base de datos local.", // Descripción actualizada
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Anime encontrado"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Anime no encontrado con el ID local proporcionado" // Descripción actualizada
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "ID local inválido (ej. no positivo)" // Nueva respuesta para IDs inválidos
                    )
            }
    )
    public ResponseEntity<AniverseResponse<AnimeDTO>> getAnimeByLocalId( // Nombre del método cambiado para claridad
                                                                         @Parameter(description = "ID del anime en la base de datos local (debe ser positivo)", required = true) // Descripción actualizada
                                                                         @PathVariable Long id) {
        try {
            log.info("Solicitud para obtener anime por ID local: {}", id);
            // Llama al método de servicio que SOLO busca localmente
            Anime anime = animeService.getAnimeById(id);
            AnimeDTO animeDTO = convertToDTO(anime);
            return ResponseEntity.ok(AniverseResponse.success("Anime local encontrado", animeDTO));
        } catch (ResourceNotFoundException e) {
            log.warn("No se encontró anime con ID local {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) { // Captura el error de ID inválido del servicio
            log.warn("Intento de búsqueda con ID local inválido {}: {}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error inesperado al obtener anime con ID local {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al procesar la solicitud: " + e.getMessage()));
        }


    }
}