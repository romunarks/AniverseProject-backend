package com.aniverse.backend.controller;

import com.aniverse.backend.dto.UsuarioStatsDTO;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.service.UsuarioStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/usuarios")
@Tag(name = "Usuario Stats", description = "API para obtener estadísticas de usuarios")
public class UsuarioStatsController {

    private final UsuarioStatsService usuarioStatsService;

    public UsuarioStatsController(UsuarioStatsService usuarioStatsService) {
        this.usuarioStatsService = usuarioStatsService;
    }

    @GetMapping("/{usuarioId}/stats")
    @Operation(
            summary = "Obtener estadísticas de un usuario",
            description = "Retorna estadísticas completas de un usuario incluyendo favoritos, reseñas, votaciones, etc.",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Estadísticas obtenidas correctamente"
                    ),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Usuario no encontrado"
                    )
            }
    )
    public ResponseEntity<AniverseResponse<UsuarioStatsDTO>> getUsuarioStats(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable Long usuarioId) {

        try {
            UsuarioStatsDTO stats = usuarioStatsService.getUsuarioStats(usuarioId);
            return ResponseEntity.ok(
                    AniverseResponse.success("Estadísticas obtenidas con éxito", stats));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener estadísticas: " + e.getMessage()));
        }
    }

    @GetMapping("/{usuarioId}/resenyas")
    @Operation(
            summary = "Obtener reseñas de un usuario",
            description = "Retorna las reseñas escritas por un usuario específico"
    )
    public ResponseEntity<AniverseResponse<Object>> getUsuarioResenyas(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            // Aquí llamarías al servicio correspondiente
            // Por ahora devolvemos un placeholder
            return ResponseEntity.ok(
                    AniverseResponse.success("Reseñas obtenidas con éxito", null));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener reseñas: " + e.getMessage()));
        }
    }
}