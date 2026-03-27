package com.aniverse.backend.controller;

import com.aniverse.backend.dto.ActividadDTO;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.repository.UsuarioRepository;
import com.aniverse.backend.service.ActividadService;
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

@RestController
@RequestMapping("/api/actividades")
@Tag(name = "Actividades", description = "API para gestionar el feed de actividades")
public class ActividadController {

    private final ActividadService actividadService;
    private final UsuarioRepository usuarioRepository;

    public ActividadController(ActividadService actividadService, UsuarioRepository usuarioRepository) {
        this.actividadService = actividadService;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping("/feed")
    @Operation(
            summary = "Obtener feed de actividades",
            description = "Retorna el feed de actividades de usuarios seguidos por el usuario actual"
    )
    public ResponseEntity<AniverseResponse<Page<ActividadDTO>>> getFeedActividades(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AniverseResponse.error("Usuario no autenticado"));
        }

        try {
            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmailAndEliminadoFalse(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha"));
            Page<ActividadDTO> actividades = actividadService.getFeedActividades(usuario.getId(), pageable);

            return ResponseEntity.ok(AniverseResponse.success("Feed de actividades obtenido con éxito", actividades));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(
            summary = "Obtener actividades de un usuario",
            description = "Retorna las actividades de un usuario específico"
    )
    public ResponseEntity<AniverseResponse<Page<ActividadDTO>>> getActividadesUsuario(
            @PathVariable Long usuarioId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha"));
            Page<ActividadDTO> actividades = actividadService.getActividadesUsuario(usuarioId, pageable);

            return ResponseEntity.ok(AniverseResponse.success("Actividades del usuario obtenidas con éxito", actividades));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/anime/{animeId}")
    @Operation(
            summary = "Obtener actividades relacionadas con un anime",
            description = "Retorna las actividades relacionadas con un anime específico"
    )
    public ResponseEntity<AniverseResponse<Page<ActividadDTO>>> getActividadesAnime(
            @PathVariable Long animeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fecha"));
            Page<ActividadDTO> actividades = actividadService.getActividadesAnime(animeId, pageable);

            return ResponseEntity.ok(AniverseResponse.success("Actividades del anime obtenidas con éxito", actividades));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/feed/personalizado")
    @Operation(
            summary = "Obtener feed personalizado de actividades",
            description = "Retorna el feed personalizado según intereses del usuario actual"
    )
    public ResponseEntity<AniverseResponse<Page<ActividadDTO>>> getFeedPersonalizado(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AniverseResponse.error("Usuario no autenticado"));
        }

        try {
            String email = authentication.getName();
            Usuario usuario = usuarioRepository.findByEmailAndEliminadoFalse(email)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + email));

            Pageable pageable = PageRequest.of(page, size);
            Page<ActividadDTO> actividades = actividadService.getFeedPersonalizado(usuario.getId(), pageable);

            return ResponseEntity.ok(AniverseResponse.success("Feed personalizado obtenido con éxito", actividades));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }
}