package com.aniverse.backend.controller;

import com.aniverse.backend.dto.FavoritoDTO;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.exception.DuplicateResourceException;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.service.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/favoritos")
@Tag(name = "Favoritos", description = "API para gestionar animes favoritos de usuarios")
public class FavoritoController {

    private static final Logger log = LoggerFactory.getLogger(FavoritoController.class);
    private final FavoritoService favoritoService;

    public FavoritoController(FavoritoService favoritoService) {
        this.favoritoService = favoritoService;
    }

    // ===============================================
    // ENDPOINTS PÚBLICOS (CONSULTA)
    // ===============================================

    @GetMapping
    @Operation(
            summary = "Obtener todos los favoritos",
            description = "Retorna una lista paginada de todos los favoritos (público para administradores)",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
            }
    )
    public ResponseEntity<AniverseResponse<Page<FavoritoDTO>>> getAllFavoritos(
            @Parameter(description = "Número de página") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Tamaño de página") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Campo de ordenamiento") @RequestParam(defaultValue = "fechaAgregado") String sortBy,
            @Parameter(description = "Dirección (asc, desc)") @RequestParam(defaultValue = "desc") String direction) {

        try {
            Page<FavoritoDTO> favoritos = favoritoService.getAllFavoritos(
                    org.springframework.data.domain.PageRequest.of(page, size,
                            org.springframework.data.domain.Sort.by(
                                    direction.equalsIgnoreCase("asc") ?
                                            org.springframework.data.domain.Sort.Direction.ASC :
                                            org.springframework.data.domain.Sort.Direction.DESC, sortBy))
            );

            return ResponseEntity.ok(AniverseResponse.success("Favoritos obtenidos con éxito", favoritos));
        } catch (Exception e) {
            log.error("Error obteniendo todos los favoritos: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(
            summary = "Obtener favoritos de un usuario",
            description = "Retorna los animes favoritos de un usuario específico"
    )
    public ResponseEntity<AniverseResponse<Page<FavoritoDTO>>> getFavoritosByUsuario(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Page<FavoritoDTO> favoritos = favoritoService.getFavoritosByUsuario(usuarioId, page, size);
            return ResponseEntity.ok(AniverseResponse.success("Favoritos del usuario obtenidos con éxito", favoritos));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error obteniendo favoritos del usuario {}: {}", usuarioId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    // ===============================================
    // ENDPOINTS AUTENTICADOS (GESTIÓN DE FAVORITOS)
    // ===============================================

    @PostMapping("/agregar")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Agregar anime a favoritos",
            description = "Agrega un anime a los favoritos del usuario autenticado",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Anime agregado a favoritos"),
                    @ApiResponse(responseCode = "409", description = "El anime ya está en favoritos"),
                    @ApiResponse(responseCode = "404", description = "Usuario o anime no encontrado")
            }
    )
    public ResponseEntity<AniverseResponse<FavoritoDTO>> agregarFavorito(
            @Valid @RequestBody Map<String, Object> request,
            HttpServletRequest servletRequest) {

        try {
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            // Extraer datos del request
            Long animeId = request.get("animeId") != null ?
                    Long.valueOf(request.get("animeId").toString()) : null;
            Long jikanId = request.get("jikanId") != null ?
                    Long.valueOf(request.get("jikanId").toString()) : null;

            if (animeId == null && jikanId == null) {
                return ResponseEntity.badRequest()
                        .body(AniverseResponse.error("Se requiere animeId o jikanId"));
            }

            FavoritoDTO favorito = favoritoService.agregarFavorito(userId, animeId, jikanId);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AniverseResponse.success("Anime agregado a favoritos exitosamente", favorito));

        } catch (DuplicateResourceException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error agregando favorito: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @PostMapping("/toggle")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Alternar estado de favorito")
    public ResponseEntity<AniverseResponse<Map<String, Object>>> toggleFavorito(
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {

        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            if (!payload.containsKey("jikanId")) {
                return ResponseEntity.badRequest()
                        .body(AniverseResponse.error("jikanId es requerido"));
            }

            Long jikanId = Long.valueOf(payload.get("jikanId").toString());

            // AQUI ESTÁ EL CAMBIO: Ya no le pasamos el 'payload' completo, solo el jikanId
            Map<String, Object> result = favoritoService.toggleFavorito(userId, jikanId);

            return ResponseEntity.ok(AniverseResponse.success("Operación completada", result));

        } catch (Exception e) {
            log.error("Error en toggle favorito: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }
    // ===============================================
    // ENDPOINTS GET PARA VERIFICAR FAVORITOS
    // ===============================================

    /**
     * Verifica si un anime es favorito usando GET con path parameter
     * Este endpoint resuelve el error NoResourceFoundException del frontend
     */
    @GetMapping("/check/{jikanId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Verificar favorito por JikanId (GET)",
            description = "Verifica si un anime específico está en los favoritos del usuario usando GET",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Verificación completada"),
                    @ApiResponse(responseCode = "401", description = "Usuario no autenticado")
            }
    )
    public ResponseEntity<AniverseResponse<Map<String, Object>>> checkFavoritoByGet(
            @PathVariable Long jikanId,
            HttpServletRequest servletRequest) {

        try {
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            log.info("Verificando favorito via GET - Usuario: {}, JikanId: {}", userId, jikanId);

            boolean isFavorite = favoritoService.existeFavoritoPorJikanId(userId, jikanId);

            Map<String, Object> response = Map.of(
                    "isFavorite", isFavorite,
                    "jikanId", jikanId,
                    "userId", userId
            );

            return ResponseEntity.ok(AniverseResponse.success("Verificación completada", response));

        } catch (Exception e) {
            log.error("Error verificando favorito via GET - JikanId: {}, Error: {}", jikanId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    /**
     * Endpoint GET simplificado que retorna solo boolean (para casos simples)
     */
    @GetMapping("/is-favorite/{jikanId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "¿Es favorito? (respuesta simple)",
            description = "Retorna solo true/false si el anime es favorito"
    )
    public ResponseEntity<Boolean> isFavoriteSimple(
            @PathVariable Long jikanId,
            HttpServletRequest servletRequest) {

        try {
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(false);
            }

            boolean isFavorite = favoritoService.existeFavoritoPorJikanId(userId, jikanId);
            return ResponseEntity.ok(isFavorite);

        } catch (Exception e) {
            log.error("Error en verificación simple - JikanId: {}: {}", jikanId, e.getMessage());
            return ResponseEntity.ok(false); // En caso de error, asumir que no es favorito
        }
    }

    /**
     * Verificar múltiples favoritos de una vez (optimización para listas)
     */
    @PostMapping("/check-multiple")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Verificar múltiples favoritos",
            description = "Verifica el estado de favorito de múltiples animes de una vez"
    )
    public ResponseEntity<AniverseResponse<Map<String, Object>>> checkMultipleFavorites(
            @RequestBody Map<String, Object> request,
            HttpServletRequest servletRequest) {

        try {
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            @SuppressWarnings("unchecked")
            List<Long> jikanIds = (List<Long>) request.get("jikanIds");

            if (jikanIds == null || jikanIds.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(AniverseResponse.error("Se requiere lista de jikanIds"));
            }

            Map<String, Boolean> results = new HashMap<>();

            for (Long jikanId : jikanIds) {
                try {
                    boolean isFavorite = favoritoService.existeFavoritoPorJikanId(userId, jikanId);
                    results.put(jikanId.toString(), isFavorite);
                } catch (Exception e) {
                    log.warn("Error verificando favorito individual {}: {}", jikanId, e.getMessage());
                    results.put(jikanId.toString(), false);
                }
            }

            Map<String, Object> response = Map.of(
                    "results", results,
                    "total", jikanIds.size(),
                    "processed", results.size()
            );

            return ResponseEntity.ok(AniverseResponse.success("Verificaciones completadas", response));

        } catch (Exception e) {
            log.error("Error en verificación múltiple: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    // ===============================================
    // OTROS ENDPOINTS EXISTENTES
    // ===============================================

    @PostMapping("/check")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Verificar si un anime es favorito",
            description = "Verifica si un anime específico está en los favoritos del usuario"
    )
    public ResponseEntity<AniverseResponse<Map<String, Object>>> checkFavorito(
            @RequestBody Map<String, Object> request,
            HttpServletRequest servletRequest) {

        try {
            Long userId = (Long) servletRequest.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            if (!request.containsKey("jikanId")) {
                return ResponseEntity.badRequest()
                        .body(AniverseResponse.error("jikanId es requerido"));
            }

            Long jikanId = Long.valueOf(request.get("jikanId").toString());
            boolean isFavorite = favoritoService.existeFavoritoPorJikanId(userId, jikanId);

            Map<String, Object> response = Map.of("isFavorite", isFavorite);
            return ResponseEntity.ok(AniverseResponse.success("Verificación completada", response));

        } catch (Exception e) {
            log.error("Error verificando favorito: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @GetMapping("/mis-favoritos")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Obtener mis favoritos",
            description = "Retorna los animes favoritos del usuario autenticado"
    )
    public ResponseEntity<AniverseResponse<Page<FavoritoDTO>>> getMisFavoritos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            Page<FavoritoDTO> favoritos = favoritoService.getFavoritosByUsuario(userId, page, size);
            return ResponseEntity.ok(AniverseResponse.success("Tus favoritos obtenidos con éxito", favoritos));

        } catch (Exception e) {
            log.error("Error obteniendo favoritos del usuario: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @DeleteMapping("/{jikanId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Eliminar favorito por JikanId",
            description = "Elimina un anime de favoritos usando su ID de Jikan"
    )
    public ResponseEntity<AniverseResponse<String>> eliminarFavoritoPorJikanId(
            @PathVariable Long jikanId,
            HttpServletRequest request) {

        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            boolean eliminado = favoritoService.eliminarFavoritoPorJikanId(userId, jikanId);

            if (eliminado) {
                return ResponseEntity.ok(AniverseResponse.success("Favorito eliminado correctamente"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AniverseResponse.error("Favorito no encontrado"));
            }

        } catch (Exception e) {
            log.error("Error eliminando favorito: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @DeleteMapping("/interno/{favoritoId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Eliminar favorito por ID interno",
            description = "Elimina un favorito usando su ID interno de base de datos"
    )
    public ResponseEntity<AniverseResponse<String>> eliminarFavorito(
            @PathVariable Long favoritoId,
            HttpServletRequest request) {

        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            favoritoService.eliminarFavorito(favoritoId);
            return ResponseEntity.ok(AniverseResponse.success("Favorito eliminado correctamente"));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            log.error("Error eliminando favorito por ID: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @GetMapping("/check-jikan/{jikanId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "¿Está en mis favoritos? (por JikanId)",
            description = "Devuelve true si el anime con ese JikanId pertenece a los favoritos del usuario autenticado",
            responses = {
                    @ApiResponse(responseCode = "200",
                            description = "Consulta realizada",
                            content = @Content(schema = @Schema(implementation = Boolean.class)))
            }
    )
    public ResponseEntity<AniverseResponse<Map<String, Object>>> isFavoritoPorJikanId(
            @PathVariable Long jikanId,
            HttpServletRequest servletRequest) {

        Long userId = (Long) servletRequest.getAttribute("userId");
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AniverseResponse.error("Usuario no autenticado"));
        }

        boolean existe = favoritoService.existeFavoritoPorJikanId(userId, jikanId);
        Map<String, Object> body = Map.of("isFavorite", existe, "jikanId", jikanId);

        return ResponseEntity.ok(AniverseResponse.success("Consulta completada", body));
    }

    // ===============================================
    // ENDPOINTS PARA RESEÑAS (VALIDACIÓN)
    // ===============================================

    @GetMapping("/animes-para-resenar")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Obtener animes favoritos para reseñar",
            description = "Retorna la lista de animes favoritos del usuario que puede reseñar"
    )
    public ResponseEntity<AniverseResponse<java.util.List<com.aniverse.backend.model.Anime>>> getAnimesParaResenar(
            HttpServletRequest request) {

        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            java.util.List<com.aniverse.backend.model.Anime> animesFavoritos =
                    favoritoService.getAnimesFavoritosUsuario(userId);

            return ResponseEntity.ok(AniverseResponse.success(
                    "Animes favoritos obtenidos para reseñar", animesFavoritos));

        } catch (Exception e) {
            log.error("Error obteniendo animes para reseñar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    @GetMapping("/puede-resenar/{animeId}")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Verificar si puede reseñar anime",
            description = "Verifica si el usuario puede reseñar un anime específico (debe estar en favoritos)"
    )
    public ResponseEntity<AniverseResponse<Map<String, Object>>> puedeResenarAnime(
            @PathVariable Long animeId,
            @RequestParam(required = false) Long jikanId,
            HttpServletRequest request) {

        try {
            Long userId = (Long) request.getAttribute("userId");
            if (userId == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(AniverseResponse.error("Usuario no autenticado"));
            }

            // Verificar usando jikanId si está disponible
            boolean puedeResenar = false;
            if (jikanId != null) {
                puedeResenar = favoritoService.existeFavoritoPorJikanId(userId, jikanId);
            }

            Map<String, Object> response = Map.of(
                    "puedeResenar", puedeResenar,
                    "motivo", puedeResenar ? "Anime está en favoritos" : "Anime no está en favoritos"
            );

            return ResponseEntity.ok(AniverseResponse.success("Verificación completada", response));

        } catch (Exception e) {
            log.error("Error verificando si puede reseñar: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }
}