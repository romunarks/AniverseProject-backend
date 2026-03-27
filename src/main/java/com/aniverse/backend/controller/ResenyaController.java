package com.aniverse.backend.controller;

import com.aniverse.backend.dto.*;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Resenya;
import com.aniverse.backend.service.ResenyaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resenyas")
@Tag(name = "Reseñas", description = "API para gestionar reseñas de animes - SOLO ANIMES FAVORITOS")
public class ResenyaController {

    private static final Logger log = LoggerFactory.getLogger(ResenyaController.class);
    private final ResenyaService resenyaService;

    public ResenyaController(ResenyaService resenyaService) {
        this.resenyaService = resenyaService;
    }

    // ===============================================
    // ENDPOINTS PÚBLICOS (CONSULTA)
    // ===============================================

    @GetMapping
    @Operation(summary = "Obtener todas las reseñas",
            description = "Retorna una lista paginada de todas las reseñas activas")
    public ResponseEntity<AniverseResponse<Page<ResenyaDTO>>> getAllResenyas(
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de la página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "fechaCreacion") String sortBy,
            @Parameter(description = "Dirección (asc, desc)") @RequestParam(defaultValue = "desc") String direction) {

        try {
            Sort.Direction dir = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
            Page<ResenyaDTO> resenyas = resenyaService.getAllResenyas(
                    PageRequest.of(page, size, Sort.by(dir, sortBy))
            );
            return ResponseEntity.ok(AniverseResponse.success("Reseñas obtenidas con éxito", resenyas));
        } catch (Exception e) {
            log.error("Error obteniendo todas las reseñas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @GetMapping("/anime/{animeId}")
    @Operation(summary = "Obtener reseñas de un anime específico con paginación")
    public ResponseEntity<AniverseResponse<Page<ResenyaDTO>>> getResenyasByAnime(
            @PathVariable Long animeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        try {
            Page<ResenyaDTO> resenyas = resenyaService.getResenyasByAnime(
                    animeId, PageRequest.of(page, size)
            );
            return ResponseEntity.ok(AniverseResponse.success("Reseñas del anime obtenidas con éxito", resenyas));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo reseñas del anime {}: {}", animeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }



    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Obtener reseñas de un usuario específico")
    public ResponseEntity<AniverseResponse<Page<ResenyaDTO>>> getResenyasByUsuario(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Page<ResenyaDTO> resenyas = resenyaService.getResenyasByUsuario(
                    usuarioId, PageRequest.of(page, size)
            );
            return ResponseEntity.ok(AniverseResponse.success("Reseñas del usuario obtenidas con éxito", resenyas));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo reseñas del usuario {}: {}", usuarioId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @GetMapping("/anime/{animeId}/estadisticas")
    @Operation(summary = "Obtener estadísticas de reseñas de un anime")
    public ResponseEntity<AniverseResponse<Map<String, Object>>> getEstadisticasAnime(
            @PathVariable Long animeId) {
        try {
            Map<String, Object> estadisticas = resenyaService.getEstadisticasAnime(animeId);
            return ResponseEntity.ok(AniverseResponse.success("Estadísticas obtenidas con éxito", estadisticas));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo estadísticas del anime {}: {}", animeId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener estadísticas"));
        }
    }

    // ===============================================
    // ENDPOINTS AUTENTICADOS - GESTIÓN DE RESEÑAS
    // ===============================================

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Crear nueva reseña",
            description = "Crea una reseña para un anime. ⚠️ IMPORTANTE: El anime debe estar en favoritos del usuario."
    )
    public ResponseEntity<AniverseResponse<ResenyaDTO>> crearResenya(
            @Valid @RequestBody ResenyaCrearDTO resenyaDTO,
            HttpServletRequest request) {

        try {
            Long usuarioId = (Long) request.getAttribute("userId");
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            // Validar que al menos uno de los IDs esté presente
            if (resenyaDTO.getAnimeId() == null && resenyaDTO.getJikanId() == null) {
                return ResponseEntity.badRequest()
                        .body(AniverseResponse.error("Se requiere animeId o jikanId"));
            }

            log.info("Creando reseña - Usuario: {}, AnimeId: {}, JikanId: {}",
                    usuarioId, resenyaDTO.getAnimeId(), resenyaDTO.getJikanId());

            ResenyaDTO nuevaResenya = resenyaService.crearResenya(usuarioId, resenyaDTO);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AniverseResponse.success("Reseña creada con éxito", nuevaResenya));

        } catch (IllegalArgumentException e) {
            // Esto captura tanto la validación de favoritos como duplicados
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error creando reseña: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @PutMapping("/{resenyaId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Actualizar reseña existente")
    public ResponseEntity<AniverseResponse<ResenyaDTO>> actualizarResenya(
            @PathVariable Long resenyaId,
            @Valid @RequestBody ResenyaActualizarDTO updateDTO,
            HttpServletRequest request) {

        try {
            Long usuarioId = (Long) request.getAttribute("userId");
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            ResenyaDTO resenyaActualizada = resenyaService.actualizarResenya(resenyaId, usuarioId, updateDTO);
            return ResponseEntity.ok(AniverseResponse.success("Reseña actualizada con éxito", resenyaActualizada));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error actualizando reseña {}: {}", resenyaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @DeleteMapping("/{resenyaId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Eliminar reseña")
    public ResponseEntity<AniverseResponse<String>> eliminarResenya(
            @PathVariable Long resenyaId,
            HttpServletRequest request) {

        try {
            Long usuarioId = (Long) request.getAttribute("userId");
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            resenyaService.eliminarResenya(resenyaId, usuarioId);
            return ResponseEntity.ok(AniverseResponse.success("Reseña eliminada con éxito"));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error eliminando reseña {}: {}", resenyaId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    // ===============================================
    // ENDPOINTS PARA VALIDACIÓN DE FAVORITOS
    // ===============================================

    @GetMapping("/mis-resenyas")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Obtener mis reseñas")
    public ResponseEntity<AniverseResponse<Page<ResenyaDTO>>> getMisResenyas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        try {
            Long usuarioId = (Long) request.getAttribute("userId");
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            Page<ResenyaDTO> misResenyas = resenyaService.getResenyasByUsuario(
                    usuarioId, PageRequest.of(page, size)
            );
            return ResponseEntity.ok(AniverseResponse.success("Tus reseñas obtenidas con éxito", misResenyas));

        } catch (Exception e) {
            log.error("Error obteniendo reseñas del usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @GetMapping("/animes-favoritos-para-resenar")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Obtener animes favoritos disponibles para reseñar",
            description = "Retorna la lista de animes favoritos del usuario que puede reseñar"
    )
    public ResponseEntity<AniverseResponse<List<Anime>>> getAnimesFavoritosParaResenar(
            HttpServletRequest request) {

        try {
            Long usuarioId = (Long) request.getAttribute("userId");
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            List<Anime> animesFavoritos = resenyaService.getAnimesFavoritosParaResenar(usuarioId);
            return ResponseEntity.ok(AniverseResponse.success(
                    "Animes favoritos disponibles para reseñar obtenidos", animesFavoritos));

        } catch (Exception e) {
            log.error("Error obteniendo animes favoritos para reseñar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @GetMapping("/puede-resenar")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Verificar si puede reseñar un anime",
            description = "Verifica si el usuario puede reseñar un anime específico (debe estar en favoritos y no haber sido reseñado)"
    )
    public ResponseEntity<AniverseResponse<Map<String, Object>>> puedeResenarAnime(
            @RequestParam(required = false) Long animeId,
            @RequestParam(required = false) Long jikanId,
            HttpServletRequest request) {

        try {
            Long usuarioId = (Long) request.getAttribute("userId");
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            if (animeId == null && jikanId == null) {
                return ResponseEntity.badRequest()
                        .body(AniverseResponse.error("Se requiere animeId o jikanId"));
            }

            boolean puedeResenar = resenyaService.puedeResenarAnime(usuarioId, animeId, jikanId);
            String motivo;

            if (puedeResenar) {
                motivo = "El anime está en favoritos y no ha sido reseñado";
            } else {
                // Verificar la razón específica
                boolean yaReseno = resenyaService.usuarioYaResenoAnime(usuarioId, animeId, jikanId);
                if (yaReseno) {
                    motivo = "Ya has reseñado este anime";
                } else {
                    motivo = "El anime no está en tus favoritos. Agrégalo primero para poder reseñarlo";
                }
            }

            Map<String, Object> response = Map.of(
                    "puedeResenar", puedeResenar,
                    "motivo", motivo
            );

            return ResponseEntity.ok(AniverseResponse.success("Verificación completada", response));

        } catch (Exception e) {
            log.error("Error verificando si puede reseñar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @GetMapping("/check-ya-reseno")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Verificar si usuario ya reseñó un anime")
    public ResponseEntity<AniverseResponse<Map<String, Object>>> checkYaReseno(
            @RequestParam(required = false) Long animeId,
            @RequestParam(required = false) Long jikanId,
            HttpServletRequest request) {

        try {
            Long usuarioId = (Long) request.getAttribute("userId");
            if (usuarioId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            boolean yaReseno = resenyaService.usuarioYaResenoAnime(usuarioId, animeId, jikanId);
            Map<String, Object> response = Map.of("yaReseno", yaReseno);

            return ResponseEntity.ok(AniverseResponse.success("Verificación completada", response));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error verificando si ya reseñó: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    // ===============================================
    // ENDPOINTS ADMINISTRATIVOS
    // ===============================================

    @GetMapping("/eliminadas")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Obtener reseñas eliminadas (Admin)")
    public ResponseEntity<AniverseResponse<Page<ResenyaDTO>>> getDeletedResenyas(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Page<ResenyaDTO> resenyasEliminadas = resenyaService.getDeletedResenyas(
                    PageRequest.of(page, size)
            );
            return ResponseEntity.ok(AniverseResponse.success("Reseñas eliminadas obtenidas con éxito", resenyasEliminadas));
        } catch (Exception e) {
            log.error("Error obteniendo reseñas eliminadas: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @PostMapping("/{resenyaId}/restaurar")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Restaurar reseña eliminada (Admin)")
    public ResponseEntity<AniverseResponse<ResenyaDTO>> restoreResenya(@PathVariable Long resenyaId) {
        try {
            Resenya resenyaRestaurada = resenyaService.restoreResenya(resenyaId);
            ResenyaDTO resenyaDTO = resenyaService.convertToDTO(resenyaRestaurada);

            return ResponseEntity.ok(AniverseResponse.success("Reseña restaurada con éxito", resenyaDTO));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error restaurando reseña: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }
}