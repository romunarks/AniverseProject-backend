package com.aniverse.backend.controller;

import com.aniverse.backend.dto.SeguidorDTO;
import com.aniverse.backend.dto.UsuarioResumidoDTO;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.repository.UsuarioRepository;
import com.aniverse.backend.service.SeguidorService;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/seguidores")
@Tag(name = "Seguidores", description = "API para gestionar relaciones de seguimiento entre usuarios")
public class SeguidorController {

    private final SeguidorService seguidorService;
    private final UsuarioRepository usuarioRepository;

    public SeguidorController(SeguidorService seguidorService, UsuarioRepository usuarioRepository) {
        this.seguidorService = seguidorService;
        this.usuarioRepository = usuarioRepository;
    }

    @PostMapping("/{seguidoId}")
    @Operation(
            summary = "Seguir a un usuario",
            description = "Establece una relación de seguimiento de usuario actual al especificado"
    )
    public ResponseEntity<AniverseResponse<SeguidorDTO>> seguirUsuario(
            @PathVariable Long seguidoId,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AniverseResponse.error("Usuario no autenticado"));
        }

        try {
            String email = authentication.getName();
            Usuario usuarioActual = usuarioRepository.findByEmailAndEliminadoFalse(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

            Long seguidorId = usuarioActual.getId();

            SeguidorDTO resultado = seguidorService.seguirUsuario(seguidorId, seguidoId);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AniverseResponse.success("Ahora sigues a este usuario", resultado));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @DeleteMapping("/{seguidoId}")
    @Operation(
            summary = "Dejar de seguir a un usuario",
            description = "Elimina una relación de seguimiento existente"
    )
    public ResponseEntity<AniverseResponse<String>> dejarDeSeguir(
            @PathVariable Long seguidoId,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AniverseResponse.error("Usuario no autenticado"));
        }

        try {
            String email = authentication.getName();
            Usuario usuarioActual = usuarioRepository.findByEmailAndEliminadoFalse(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

            Long seguidorId = usuarioActual.getId();

            seguidorService.dejarDeSeguir(seguidorId, seguidoId);
            return ResponseEntity.ok(AniverseResponse.success("Has dejado de seguir a este usuario"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/usuario/{usuarioId}/seguidos")
    @Operation(
            summary = "Obtener usuarios seguidos",
            description = "Retorna la lista de usuarios a los que sigue un usuario específico"
    )
    public ResponseEntity<AniverseResponse<Page<UsuarioResumidoDTO>>> obtenerSeguidos(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long usuarioActualId = null;
        if (authentication != null) {
            String email = authentication.getName();
            usuarioActualId = usuarioRepository.findByEmailAndEliminadoFalse(email)
                    .map(Usuario::getId)
                    .orElse(null);
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaSeguimiento"));
            Page<UsuarioResumidoDTO> seguidos = seguidorService.obtenerSeguidos(usuarioId, pageable, usuarioActualId);
            return ResponseEntity.ok(AniverseResponse.success("Usuarios seguidos obtenidos con éxito", seguidos));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/usuario/{usuarioId}/seguidores")
    @Operation(
            summary = "Obtener seguidores",
            description = "Retorna la lista de usuarios que siguen a un usuario específico"
    )
    public ResponseEntity<AniverseResponse<Page<UsuarioResumidoDTO>>> obtenerSeguidores(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication authentication) {

        Long usuarioActualId = null;
        if (authentication != null) {
            String email = authentication.getName();
            usuarioActualId = usuarioRepository.findByEmailAndEliminadoFalse(email)
                    .map(Usuario::getId)
                    .orElse(null);
        }

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fechaSeguimiento"));
            Page<UsuarioResumidoDTO> seguidores = seguidorService.obtenerSeguidores(usuarioId, pageable, usuarioActualId);
            return ResponseEntity.ok(AniverseResponse.success("Seguidores obtenidos con éxito", seguidores));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/usuario/{usuarioId}/mutuos")
    @Operation(
            summary = "Obtener seguidores mutuos",
            description = "Retorna la lista de usuarios que siguen al usuario especificado y que también son seguidos por él"
    )
    public ResponseEntity<AniverseResponse<Page<UsuarioResumidoDTO>>> obtenerSeguidoresMutuos(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<UsuarioResumidoDTO> mutuos = seguidorService.obtenerSeguidoresMutuos(usuarioId, pageable);
            return ResponseEntity.ok(AniverseResponse.success("Seguidores mutuos obtenidos con éxito", mutuos));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/verificar/{seguidoId}")
    @Operation(
            summary = "Verificar si el usuario actual sigue a otro",
            description = "Verifica si existe una relación de seguimiento del usuario actual al especificado"
    )
    public ResponseEntity<AniverseResponse<Map<String, Boolean>>> verificarSeguimiento(
            @PathVariable Long seguidoId,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AniverseResponse.error("Usuario no autenticado"));
        }

        try {
            String email = authentication.getName();
            Usuario usuarioActual = usuarioRepository.findByEmailAndEliminadoFalse(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

            Long seguidorId = usuarioActual.getId();

            boolean siguiendo = seguidorService.verificarSeguimiento(seguidorId, seguidoId);

            Map<String, Boolean> respuesta = new HashMap<>();
            respuesta.put("siguiendo", siguiendo);

            return ResponseEntity.ok(AniverseResponse.success("Verificación realizada con éxito", respuesta));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/usuario/{usuarioId}/estadisticas")
    @Operation(
            summary = "Obtener estadísticas de seguimiento",
            description = "Retorna estadísticas de seguidores y seguidos de un usuario"
    )
    public ResponseEntity<AniverseResponse<Map<String, Long>>> obtenerEstadisticas(
            @PathVariable Long usuarioId) {

        try {
            Map<String, Long> estadisticas = seguidorService.obtenerEstadisticasSeguimiento(usuarioId);
            return ResponseEntity.ok(AniverseResponse.success("Estadísticas obtenidas con éxito", estadisticas));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }
}