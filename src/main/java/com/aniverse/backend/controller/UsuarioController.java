package com.aniverse.backend.controller;

import com.aniverse.backend.dto.*;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.exception.DuplicateResourceException;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.repository.UsuarioRepository;
import com.aniverse.backend.service.UsuarioService;
import com.aniverse.backend.service.UsuarioStatsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    public static final Logger log = LoggerFactory.getLogger(UsuarioController.class);


    private final UsuarioService usuarioService;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioStatsService usuarioStatsService;

    public UsuarioController(UsuarioService usuarioService,
                             UsuarioRepository usuarioRepository, UsuarioStatsService usuarioStatsService) {
        this.usuarioService = usuarioService;
        this.usuarioRepository = usuarioRepository;
        this.usuarioStatsService = usuarioStatsService;
    }

    @GetMapping
    public ResponseEntity<AniverseResponse<Page<UsuarioDTO>>> getAllUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction dir = direction.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        Page<UsuarioDTO> usuarios = usuarioService.getAllUsuarios(pageable);

        return ResponseEntity.ok(AniverseResponse.success("Usuarios obtenidos con éxito", usuarios));
    }
    // En UsuarioController.java, modificar el endpoint getUsuarioById:

    @GetMapping("/{id}")
    public ResponseEntity<AniverseResponse<UsuarioDTO>> getUsuarioById(
            @PathVariable Long id,
            Authentication authentication) {

        Long usuarioActualId = null;
        if (authentication != null) {
            String email = authentication.getName();
            usuarioActualId = usuarioRepository.findByEmailAndEliminadoFalse(email)
                    .map(Usuario::getId)
                    .orElse(null);
        }

        try {
            UsuarioDTO usuario = usuarioService.getUsuarioById(id, usuarioActualId);
            return ResponseEntity.ok(AniverseResponse.success("Usuario encontrado con éxito", usuario));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }
    // En UsuarioController.java - Método getCurrentUsuario CORREGIDO

    /**
     * Endpoint para obtener el usuario actual autenticado.
     *
     * Ahora usa el service layer en lugar del repository directo,
     * evitando el error de Hibernate con @ElementCollection.
     *
     * @param authentication Información de autenticación del usuario actual
     * @return ResponseEntity con los datos del usuario o error si no está autenticado
     */
    @GetMapping("/me")
    public ResponseEntity<AniverseResponse<UsuarioDTO>> getCurrentUsuario(Authentication authentication) {
        // Verificar autenticación
        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(AniverseResponse.error("Usuario no autenticado"));
        }

        String email = authentication.getName(); // Obtener email del token JWT

        try {
            // SOLUCIÓN: Usar el service layer en lugar del repository directo
            UsuarioDTO usuario = usuarioService.getUsuarioByEmail(email);

            return ResponseEntity.ok(AniverseResponse.success("Usuario actual obtenido con éxito", usuario));

        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            // Log del error para debugging
            log.error("Error inesperado al obtener usuario actual con email {}: {}", email, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error interno del servidor"));
        }
    }

    /**
     *
     */
    @PutMapping("/{id}")
    public ResponseEntity<AniverseResponse<UsuarioDTO>> updateUsuario(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioActualizarDTO usuarioDTO) {
        try {
            // Obtener usuario actual sin el segundo parámetro que causa problemas
            UsuarioDTO usuarioActual = usuarioService.getUsuarioById(id);

            if (usuarioService.existsByEmail(usuarioDTO.getEmail()) &&
                    !usuarioActual.getEmail().equals(usuarioDTO.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(AniverseResponse.error("El email ya está en uso."));
            }

            UsuarioDTO usuarioActualizado = usuarioService.updateUsuario(id, usuarioDTO);
            return ResponseEntity.ok(AniverseResponse.success("Usuario actualizado con éxito", usuarioActualizado));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @PatchMapping("/{id}/contrasenya")
    public ResponseEntity<AniverseResponse<String>> updateContrasenya(
            @PathVariable Long id,
            @Valid @RequestBody UsuarioCambioContrasenyaDTO contrasenyaDTO) {
        try {
            usuarioService.updateContrasenya(id, contrasenyaDTO.getOldPassword(), contrasenyaDTO.getNewPassword());
            return ResponseEntity.ok(AniverseResponse.success("Contraseña actualizada correctamente."));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }
    @PostMapping
    public ResponseEntity<AniverseResponse<UsuarioDTO>> createUsuario(@Valid @RequestBody UsuarioRegistroDTO usuarioDTO) {
        try {
            if (usuarioService.existsByEmail(usuarioDTO.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(AniverseResponse.error("El correo ya está en uso."));
            }

            Usuario nuevoUsuario = usuarioService.saveUsuario(usuarioDTO);
            UsuarioDTO usuarioDTOResponse = new UsuarioDTO(
                    nuevoUsuario.getId(),
                    nuevoUsuario.getNombre(),
                    nuevoUsuario.getEmail(),
                    nuevoUsuario.getRoles()
            );

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AniverseResponse.success("Usuario creado con éxito", usuarioDTOResponse));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<AniverseResponse<String>> deleteUsuario(@PathVariable Long id) {
        try {
            usuarioService.softDeleteUsuario(id);
            return ResponseEntity.ok(
                    AniverseResponse.success("El usuario con ID " + id + " ha sido eliminado."));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }
    // Añadir endpoint para ver usuarios eliminados (admin) // POR AHORA USER
    @GetMapping("/eliminados")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<AniverseResponse<Page<UsuarioDTO>>> getDeletedUsuarios(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String direction) {

        Sort.Direction dir = direction.equalsIgnoreCase("asc") ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));
        Page<UsuarioDTO> usuarios = usuarioService.getDeletedUsuarios(pageable);

        return ResponseEntity.ok(AniverseResponse.success("Usuarios eliminados obtenidos con éxito", usuarios));
    }

    @PostMapping("/{id}/restaurar")
    @PreAuthorize("hasAuthority('ROLE_USER')")
    public ResponseEntity<AniverseResponse<UsuarioDTO>> restoreUsuario(@PathVariable Long id) {
        try {
            UsuarioDTO usuarioDTO = usuarioService.restoreUsuario(id);
            return ResponseEntity.ok(AniverseResponse.success("Usuario restaurado con éxito", usuarioDTO));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (IllegalStateException | DuplicateResourceException e) {
            return ResponseEntity.badRequest()
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }


}
