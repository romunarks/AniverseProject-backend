package com.aniverse.backend.controller;

import com.aniverse.backend.dto.AnimeDTO;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.service.AdvancedRecommendationService;
import com.aniverse.backend.service.RecomendacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@RestController
@RequestMapping("/api/recomendaciones")
@Tag(name = "Recomendaciones", description = "API para sistema de recomendación de animes")
public class RecomendacionController {

    private final RecomendacionService recomendacionService;
    private final AdvancedRecommendationService advancedRecommendationService;

    public RecomendacionController(RecomendacionService recomendacionService, AdvancedRecommendationService advancedRecommendationService) {
        this.recomendacionService = recomendacionService;
        this.advancedRecommendationService = advancedRecommendationService;
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(
            summary = "Obtener recomendaciones personalizadas",
            description = "Retorna animes recomendados basados en los gustos del usuario",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Recomendaciones obtenidas correctamente",
                            content = @Content(schema = @Schema(implementation = List.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuario no encontrado",
                            content = @Content
                    )
            }
    )
    public ResponseEntity<AniverseResponse<List<AnimeDTO>>> getRecomendacionesPersonalizadas(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable Long usuarioId,

            @Parameter(description = "Límite de resultados")
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AnimeDTO> recomendaciones = recomendacionService.getRecomendacionesPersonalizadas(usuarioId, limit);
            return ResponseEntity.ok(
                    AniverseResponse.success("Recomendaciones obtenidas con éxito", recomendaciones));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Usuario no encontrado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener recomendaciones: " + e.getMessage()));
        }
    }

    @GetMapping("/genero/{genero}")
    @Operation(
            summary = "Obtener los mejores animes por género",
            description = "Retorna los animes mejor puntuados de un género específico",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista obtenida correctamente",
                            content = @Content(schema = @Schema(implementation = List.class))
                    )
            }
    )
    public ResponseEntity<AniverseResponse<List<AnimeDTO>>> getTopRatedAnimesByGenre(
            @Parameter(description = "Género de anime", required = true)
            @PathVariable String genero,

            @Parameter(description = "Límite de resultados")
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AnimeDTO> animes = recomendacionService.getTopRatedAnimesByGenre(genero, limit);
            return ResponseEntity.ok(
                    AniverseResponse.success("Animes por género obtenidos con éxito", animes));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener animes por género: " + e.getMessage()));
        }
    }

    @GetMapping("/similares/{animeId}")
    @Operation(
            summary = "Obtener animes similares",
            description = "Retorna animes similares a uno específico",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista obtenida correctamente",
                            content = @Content(schema = @Schema(implementation = List.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Anime no encontrado",
                            content = @Content
                    )
            }
    )
    public ResponseEntity<AniverseResponse<List<AnimeDTO>>> getAnimesSimilares(
            @Parameter(description = "ID del anime", required = true)
            @PathVariable Long animeId,

            @Parameter(description = "Límite de resultados")
            @RequestParam(defaultValue = "5") int limit) {
        try {
            List<AnimeDTO> similares = recomendacionService.getAnimesSimilares(animeId, limit);
            return ResponseEntity.ok(
                    AniverseResponse.success("Animes similares obtenidos con éxito", similares));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Anime no encontrado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener animes similares: " + e.getMessage()));
        }
    }
    @GetMapping("/avanzadas/usuario/{usuarioId}")
    @Operation(
            summary = "Obtener recomendaciones avanzadas personalizadas",
            description = "Retorna animes recomendados usando algoritmos avanzados de content-based y collaborative filtering",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Recomendaciones obtenidas correctamente",
                            content = @Content(schema = @Schema(implementation = List.class))
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuario no encontrado",
                            content = @Content
                    )
            }
    )
    public ResponseEntity<AniverseResponse<CompletableFuture<List<AnimeDTO>>>> getAdvancedRecommendations(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable Long usuarioId,

            @Parameter(description = "Límite de resultados")
            @RequestParam(defaultValue = "10") int limit) {
        try {
            CompletableFuture<List<AnimeDTO>> recomendaciones =
                    advancedRecommendationService.getPersonalizedRecommendations(usuarioId, limit);

            return ResponseEntity.ok(
                    AniverseResponse.success("Recomendaciones avanzadas obtenidas con éxito", recomendaciones));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener recomendaciones avanzadas: " + e.getMessage()));
        }
    }

    @GetMapping("/debug/usuario/{usuarioId}")
    @Operation(
            summary = "Debug de preferencias de usuario",
            description = "Muestra información sobre las preferencias del usuario para debugging",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Información de debug obtenida")
            }
    )
    public ResponseEntity<AniverseResponse<Map<String, Object>>> debugUserPreferences(
            @PathVariable Long usuarioId) {
        try {
            // Este método lo crearemos en AdvancedRecommendationService
            Map<String, Object> debug = advancedRecommendationService.getDebugInfo(usuarioId);

            return ResponseEntity.ok(
                    AniverseResponse.success("Debug info obtenida con éxito", debug));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error en debug: " + e.getMessage()));
        }
    }
}