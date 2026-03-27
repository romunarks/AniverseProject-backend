package com.aniverse.backend.controller;

import com.aniverse.backend.dto.VotacionDTO;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.model.Votacion;
import com.aniverse.backend.repository.AnimeRepository;
import com.aniverse.backend.repository.UsuarioRepository;
import com.aniverse.backend.repository.VotacionRepository;
import com.aniverse.backend.service.AnimeService;
import com.aniverse.backend.service.UsuarioService;
import com.aniverse.backend.service.VotacionService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/votaciones")
public class VotacionController {

    private final VotacionService votacionService;
    private final VotacionRepository votacionRepository;
    private final UsuarioService usuarioService;
    private final AnimeService animeService;
    private final UsuarioRepository usuarioRepository;
    private final AnimeRepository animeRepository;

    public VotacionController(VotacionService votacionService,
                              VotacionRepository votacionRepository,
                              UsuarioService usuarioService,
                              AnimeService animeService,
                              UsuarioRepository usuarioRepository,
                              AnimeRepository animeRepository) {
        this.votacionService = votacionService;
        this.votacionRepository = votacionRepository;
        this.usuarioService = usuarioService;
        this.animeService = animeService;
        this.usuarioRepository = usuarioRepository;
        this.animeRepository = animeRepository;
    }

    // ✅ MÉTODO: Obtener todas las votaciones (admin)
    @GetMapping
    @Operation(summary = "Obtener todas las votaciones")
    public ResponseEntity<AniverseResponse<List<VotacionDTO>>> getAllVotaciones() {
        List<VotacionDTO> votaciones = votacionRepository.findAllAsDTO();
        return ResponseEntity.ok(AniverseResponse.success("Votaciones obtenidas con éxito", votaciones));
    }

    // ✅ MÉTODO: Crear votación (para el frontend)
    @PostMapping
    @Operation(summary = "Crear nueva votación")
    public ResponseEntity<Map<String, Object>> createVotacion(
            @RequestBody Map<String, Object> votacionData,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            // Obtener usuario autenticado
            Long authenticatedUserId = (Long) request.getAttribute("userId");

            if (authenticatedUserId == null) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Long jikanId = Long.valueOf(votacionData.get("jikanId").toString());
            Integer puntuacion = Integer.valueOf(votacionData.get("puntuacion").toString());

            // Validar puntuación (1-10 estrellas)
            if (puntuacion < 1 || puntuacion > 10) {
                response.put("success", false);
                response.put("message", "La puntuación debe estar entre 1 y 10");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Crear votación
            boolean created = votacionService.createVotacion(authenticatedUserId, jikanId, puntuacion);

            if (created) {
                response.put("success", true);
                response.put("message", "Votación creada correctamente");
                response.put("data", Map.of(
                        "jikanId", jikanId,
                        "puntuacion", puntuacion
                ));
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            } else {
                response.put("success", false);
                response.put("message", "No se pudo crear la votación");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("message", "Formato de datos inválido");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ✅ MÉTODO: Actualizar votación existente
    @PutMapping
    @Operation(summary = "Actualizar votación existente")
    public ResponseEntity<Map<String, Object>> updateVotacion(
            @RequestBody Map<String, Object> votacionData,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long authenticatedUserId = (Long) request.getAttribute("userId");

            if (authenticatedUserId == null) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Long jikanId = Long.valueOf(votacionData.get("jikanId").toString());
            Integer puntuacion = Integer.valueOf(votacionData.get("puntuacion").toString());

            // Validar puntuación
            if (puntuacion < 1 || puntuacion > 10) {
                response.put("success", false);
                response.put("message", "La puntuación debe estar entre 1 y 10");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

            // Actualizar votación
            boolean updated = votacionService.updateVotacion(authenticatedUserId, jikanId, puntuacion);

            if (updated) {
                response.put("success", true);
                response.put("message", "Votación actualizada correctamente");
                response.put("data", Map.of(
                        "jikanId", jikanId,
                        "puntuacion", puntuacion
                ));
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "No se pudo actualizar la votación");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (NumberFormatException e) {
            response.put("success", false);
            response.put("message", "Formato de datos inválido");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ✅ MÉTODO: Obtener votación del usuario para un anime específico
    @GetMapping("/user/{jikanId}")
    @Operation(summary = "Obtener votación del usuario para un anime específico")
    public ResponseEntity<Map<String, Object>> getUserVotacion(
            @PathVariable Long jikanId,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long authenticatedUserId = (Long) request.getAttribute("userId");

            if (authenticatedUserId == null) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            Optional<Votacion> votacion = votacionService.findByUserIdAndJikanId(authenticatedUserId, jikanId);

            Map<String, Object> data = new HashMap<>();
            if (votacion.isPresent()) {
                // Convertir puntuación interna (1-5) a externa (1-10)
                int puntuacionExterna = (int) Math.round(votacion.get().getPuntuacion() * 2);

                data.put("hasVoted", true);
                data.put("rating", puntuacionExterna);
                data.put("votacionId", votacion.get().getId());
            } else {
                data.put("hasVoted", false);
                data.put("rating", 0);
            }

            response.put("success", true);
            response.put("data", data);
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ✅ MÉTODO: Eliminar votación por jikanId
    @DeleteMapping("/{jikanId}")
    @Operation(summary = "Eliminar votación por JikanId")
    public ResponseEntity<Map<String, Object>> removeVotacion(
            @PathVariable Long jikanId,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = (Long) request.getAttribute("userId");

            if (userId == null) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            boolean removed = votacionService.removeVotacionByJikanId(userId, jikanId);

            if (removed) {
                response.put("success", true);
                response.put("message", "Votación eliminada correctamente");
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "No se encontró votación para eliminar");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ✅ MÉTODO: Obtener todas las votaciones del usuario autenticado
    @GetMapping("/user")
    @Operation(summary = "Obtener todas las votaciones del usuario autenticado")
    public ResponseEntity<Map<String, Object>> getUserVotaciones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        Map<String, Object> response = new HashMap<>();

        try {
            Long userId = (Long) request.getAttribute("userId");

            if (userId == null) {
                response.put("success", false);
                response.put("message", "Usuario no autenticado");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }

            List<VotacionDTO> votaciones = votacionService.getVotacionesByUsuario(userId, page, size);

            // Convertir puntuaciones internas (1-5) a externas (1-10)
            votaciones.forEach(votacion -> {
                double puntuacionExterna = votacion.getPuntuacion() * 2;
                votacion.setPuntuacion(puntuacionExterna);
            });

            response.put("success", true);
            response.put("data", votaciones);
            response.put("message", "Votaciones obtenidas correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ✅ MÉTODO: Obtener estadísticas de un anime
    @GetMapping("/stats/{jikanId}")
    @Operation(summary = "Obtener estadísticas de votaciones para un anime")
    public ResponseEntity<Map<String, Object>> getAnimeStats(@PathVariable Long jikanId) {

        Map<String, Object> response = new HashMap<>();

        try {
            Map<String, Object> stats = votacionService.getAnimeStatsByJikanId(jikanId);

            response.put("success", true);
            response.put("data", stats);
            response.put("message", "Estadísticas obtenidas correctamente");

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Error interno del servidor: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    // ============ MÉTODOS LEGACY PARA COMPATIBILIDAD ============

    // ✅ MÉTODO LEGACY: saveVotacion (para animes internos)
    @PostMapping("/internal")
    @Operation(summary = "Crear votación para anime interno (legacy)")
    public ResponseEntity<AniverseResponse<Map<String, Object>>> saveVotacion(@RequestBody Votacion votacion) {
        try {
            if (!usuarioRepository.existsById(votacion.getUsuario().getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AniverseResponse.error("Usuario no encontrado."));
            }
            if (!animeRepository.existsById(votacion.getAnime().getId())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(AniverseResponse.error("Anime no encontrado."));
            }

            Votacion nuevaVotacion = votacionService.saveVotacion(votacion);

            // Crear un mapa para la respuesta
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("id", nuevaVotacion.getId());
            responseData.put("usuarioId", nuevaVotacion.getUsuario().getId());
            responseData.put("usuarioNombre", nuevaVotacion.getUsuario().getNombre());
            responseData.put("animeId", nuevaVotacion.getAnime().getId());
            responseData.put("animeTitulo", nuevaVotacion.getAnime().getTitulo());
            responseData.put("puntuacion", nuevaVotacion.getPuntuacion());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AniverseResponse.success("Votación registrada con éxito", responseData));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error inesperado al registrar la votación."));
        }
    }

    // ✅ MÉTODO LEGACY: Eliminar votación por ID interno
    @DeleteMapping("/internal/{id}")
    @Operation(summary = "Eliminar votación por ID interno")
    public ResponseEntity<AniverseResponse<String>> deleteVotacion(@PathVariable Long id) {
        try {
            votacionService.deleteVotacion(id);
            return ResponseEntity.ok(
                    AniverseResponse.success("La votación con ID " + id + " ha sido eliminada."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("La votación con ID " + id + " no fue encontrada."));
        }
    }

    // ✅ MÉTODO LEGACY: Obtener votaciones por usuario
    @GetMapping("/usuario/{usuarioId}")
    @Operation(summary = "Obtener votaciones de un usuario")
    public ResponseEntity<AniverseResponse<List<VotacionDTO>>> getVotacionesByUsuario(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            List<VotacionDTO> votaciones = votacionService.getVotacionesByUsuario(usuarioId, page, size);
            return ResponseEntity.ok(AniverseResponse.success("Votaciones obtenidas con éxito", votaciones));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener votaciones: " + e.getMessage()));
        }
    }

    // ✅ MÉTODO LEGACY: Obtener votación específica de usuario para anime
    @GetMapping("/usuario/{usuarioId}/anime/{animeId}")
    @Operation(summary = "Obtener votación específica de un usuario para un anime")
    public ResponseEntity<AniverseResponse<VotacionDTO>> getVotacionUsuarioAnime(
            @PathVariable Long usuarioId,
            @PathVariable Long animeId) {

        try {
            VotacionDTO votacion = votacionService.getVotacionUsuarioAnime(usuarioId, animeId);
            if (votacion != null) {
                return ResponseEntity.ok(AniverseResponse.success("Votación encontrada", votacion));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(AniverseResponse.error("No se encontró votación para este usuario y anime"));
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener votación: " + e.getMessage()));
        }
    }

    // ✅ MÉTODO LEGACY: Obtener votación por jikanId (corregido)
    @GetMapping("/usuario/{userId}/jikan/{jikanId}")
    @Operation(summary = "Obtener votación por jikanId (legacy)")
    public ResponseEntity<AniverseResponse<VotacionDTO>> getVotacionUsuarioAnimeByJikanId(
            @PathVariable Long userId,
            @PathVariable Long jikanId,
            HttpServletRequest request) {
        try {
            // Verificar que el usuario autenticado sea el mismo o tenga permisos
            Long authenticatedUserId = (Long) request.getAttribute("userId");
            if (!authenticatedUserId.equals(userId)) {
                AniverseResponse<VotacionDTO> response = AniverseResponse.<VotacionDTO>builder()
                        .success(false)
                        .message("No tienes permisos para acceder a esta información")
                        .build();
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
            }

            VotacionDTO votacion = votacionService.getVotacionUsuarioAnimeByJikanId(userId, jikanId.intValue());

            AniverseResponse<VotacionDTO> response = AniverseResponse.<VotacionDTO>builder()
                    .success(true)
                    .message("Votación obtenida correctamente")
                    .data(votacion)
                    .build();

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            AniverseResponse<VotacionDTO> response = AniverseResponse.<VotacionDTO>builder()
                    .success(false)
                    .message("Error al obtener votación: " + e.getMessage())
                    .build();
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ✅ MÉTODO LEGACY: Obtener conteo de votaciones para un anime
    @GetMapping("/count/{animeId}")
    @Operation(summary = "Obtener cantidad de votaciones para un anime")
    public ResponseEntity<AniverseResponse<Map<String, Object>>> getVotacionesCount(
            @PathVariable Long animeId) {

        try {
            long count = votacionService.getVotacionesCountByAnime(animeId);
            Map<String, Object> result = new HashMap<>();
            result.put("count", count);
            return ResponseEntity.ok(AniverseResponse.success("Conteo obtenido con éxito", result));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener conteo: " + e.getMessage()));
        }
    }
}