package com.aniverse.backend.controller;

import com.aniverse.backend.dto.AnimeDTO;
import com.aniverse.backend.dto.EstadisticasDTO;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.service.EstadisticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/estadisticas")
@Tag(name = "Estadísticas", description = "API para obtener estadísticas y datos agregados")
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    public EstadisticaController(EstadisticaService estadisticaService) {
        this.estadisticaService = estadisticaService;
    }

    @GetMapping
    @Operation(
            summary = "Obtener estadísticas generales",
            description = "Retorna estadísticas generales de la aplicación",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Estadísticas obtenidas correctamente",
                            content = @Content(schema = @Schema(implementation = EstadisticasDTO.class))
                    )
            }
    )

    public ResponseEntity<AniverseResponse<EstadisticasDTO>> getEstadisticasGenerales() {
        try {
            EstadisticasDTO estadisticas = estadisticaService.getEstadisticasGenerales();
            return ResponseEntity.ok(
                    AniverseResponse.success("Estadísticas obtenidas con éxito", estadisticas));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AniverseResponse.error("Error al obtener estadísticas: " + e.getMessage()));
        }
    }

    @GetMapping("/anime/{animeId}")
    @Operation(summary = "Obtener estadísticas de un anime específico")
    public ResponseEntity<AniverseResponse<Map<String, Object>>> getEstadisticasAnime(
            @PathVariable Long animeId) {
        try {
            Map<String, Object> estadisticas = estadisticaService.getEstadisticasAnime(animeId);
            return ResponseEntity.ok(
                    AniverseResponse.success("Estadísticas del anime obtenidas con éxito", estadisticas));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AniverseResponse.error("Error al obtener estadísticas del anime: " + e.getMessage()));
        }
    }

    @GetMapping("/top-rated")
    @Operation(
            summary = "Obtener animes mejor puntuados",
            description = "Retorna una lista de los animes con mejor puntuación",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista obtenida correctamente",
                            content = @Content(schema = @Schema(implementation = List.class))
                    )
            }
    )
    public ResponseEntity<AniverseResponse<List<AnimeDTO>>> getTopRatedAnimes(
            @Parameter(description = "Límite de resultados")
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AnimeDTO> animes = estadisticaService.getTopRatedAnimes(limit);
            return ResponseEntity.ok(
                    AniverseResponse.success("Animes mejor puntuados obtenidos con éxito", animes));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AniverseResponse.error("Error al obtener animes mejor puntuados: " + e.getMessage()));
        }
    }

    @GetMapping("/most-recent")
    @Operation(
            summary = "Obtener animes más recientes",
            description = "Retorna una lista de los animes añadidos más recientemente",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista obtenida correctamente",
                            content = @Content(schema = @Schema(implementation = List.class))
                    )
            }
    )
    public ResponseEntity<AniverseResponse<List<AnimeDTO>>> getMostRecentAnimes(
            @Parameter(description = "Límite de resultados")
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AnimeDTO> animes = estadisticaService.getMostRecentAnimes(limit);
            return ResponseEntity.ok(
                    AniverseResponse.success("Animes más recientes obtenidos con éxito", animes));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AniverseResponse.error("Error al obtener animes recientes: " + e.getMessage()));
        }
    }

    @GetMapping("/most-voted")
    @Operation(
            summary = "Obtener animes más votados",
            description = "Retorna una lista de los animes con mayor cantidad de votaciones",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Lista obtenida correctamente",
                            content = @Content(schema = @Schema(implementation = List.class))
                    )
            }
    )
    public ResponseEntity<AniverseResponse<List<AnimeDTO>>> getMostVotedAnimes(
            @Parameter(description = "Límite de resultados")
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<AnimeDTO> animes = estadisticaService.getMostVotedAnimes(limit);
            return ResponseEntity.ok(
                    AniverseResponse.success("Animes más votados obtenidos con éxito", animes));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(AniverseResponse.error("Error al obtener animes más votados: " + e.getMessage()));
        }
    }
}