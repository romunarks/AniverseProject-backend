package com.aniverse.backend.controller;

import com.aniverse.backend.dto.NotificacionDTO;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.repository.UsuarioRepository;
import com.aniverse.backend.service.NotificacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notificaciones")
@Tag(name = "Notificaciones", description = "API para gestionar notificaciones de usuarios")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioRepository usuarioRepository;

    public NotificacionController(NotificacionService notificacionService, UsuarioRepository usuarioRepository) {
        this.notificacionService = notificacionService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    @Operation(
            summary = "Obtener notificaciones del usuario autenticado",
            description = "Retorna una lista paginada de notificaciones del usuario actual"
    )
    public ResponseEntity<AniverseResponse<Page<NotificacionDTO>>> getNotificacionesUsuario(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean soloNoLeidas,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AniverseResponse.error("Usuario no autenticado"));
        }

        try {
            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmailAndEliminadoFalse(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

            Long usuarioId = usuario.getId();

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha"));

            Page<NotificacionDTO> notificaciones;
            if (soloNoLeidas) {
                notificaciones = notificacionService.getNotificacionesNoLeidas(usuarioId, pageable);
            } else {
                notificaciones = notificacionService.getNotificacionesUsuario(usuarioId, pageable);
            }

            return ResponseEntity.ok(AniverseResponse.success(
                    "Notificaciones obtenidas con éxito", notificaciones));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener notificaciones: " + e.getMessage()));
        }
    }

    @GetMapping("/conteo")
    @Operation(
            summary = "Obtener conteo de notificaciones no leídas",
            description = "Retorna el número de notificaciones no leídas del usuario actual"
    )
    public ResponseEntity<AniverseResponse<Map<String, Long>>> getConteoNoLeidas(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AniverseResponse.error("Usuario no autenticado"));
        }

        try {
            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmailAndEliminadoFalse(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

            Long usuarioId = usuario.getId();
            long cantidad = notificacionService.contarNotificacionesNoLeidas(usuarioId);

            Map<String, Long> respuesta = new HashMap<>();
            respuesta.put("cantidadNoLeidas", cantidad);

            return ResponseEntity.ok(AniverseResponse.success("Conteo obtenido con éxito", respuesta));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener conteo: " + e.getMessage()));
        }
    }

    @PatchMapping("/{id}/leer")
    @Operation(
            summary = "Marcar notificación como leída",
            description = "Marca una notificación específica como leída"
    )
    public ResponseEntity<AniverseResponse<NotificacionDTO>> marcarComoLeida(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AniverseResponse.error("Usuario no autenticado"));
        }

        try {
            NotificacionDTO notificacion = notificacionService.marcarComoLeida(id);
            return ResponseEntity.ok(AniverseResponse.success("Notificación marcada como leída", notificacion));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Notificación no encontrada o error al marcarla: " + e.getMessage()));
        }
    }

    @PostMapping("/leer-todas")
    @Operation(
            summary = "Marcar todas las notificaciones como leídas",
            description = "Marca todas las notificaciones del usuario actual como leídas"
    )
    public ResponseEntity<AniverseResponse<Void>> marcarTodasComoLeidas(Authentication authentication) {
        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AniverseResponse.error("Usuario no autenticado"));
        }

        try {
            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmailAndEliminadoFalse(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

            Long usuarioId = usuario.getId();
            notificacionService.marcarTodasComoLeidas(usuarioId);

            return ResponseEntity.ok(AniverseResponse.success("Todas las notificaciones marcadas como leídas"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al marcar notificaciones: " + e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar notificación",
            description = "Elimina una notificación específica"
    )
    public ResponseEntity<AniverseResponse<Void>> eliminarNotificacion(
            @PathVariable Long id,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AniverseResponse.error("Usuario no autenticado"));
        }

        try {
            notificacionService.eliminarNotificacion(id);
            return ResponseEntity.ok(AniverseResponse.success("Notificación eliminada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Notificación no encontrada o error al eliminarla: " + e.getMessage()));
        }
    }

    @PostMapping("/test")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<AniverseResponse<NotificacionDTO>> crearNotificacionTest(
            @RequestParam Long destinatarioId,
            @RequestParam(required = false) Long emisorId,
            @RequestParam String tipo,
            @RequestParam String mensaje,
            @RequestParam(required = false) Long objetoId,
            @RequestParam(required = false) String objetoTipo,
            @RequestParam(required = false) String url) {

        try {
            NotificacionDTO notificacion = notificacionService.crearNotificacion(
                    destinatarioId, emisorId, tipo, mensaje, objetoId, objetoTipo, url);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AniverseResponse.success("Notificación de prueba creada con éxito", notificacion));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al crear notificación de prueba: " + e.getMessage()));
        }
    }
}