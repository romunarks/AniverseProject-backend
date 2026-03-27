package com.aniverse.backend.controller;

import com.aniverse.backend.dto.*;
import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.service.ListaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/listas")
@Tag(name = "Listas", description = "API para gestionar listas personalizadas de animes")
public class ListaController {

    private final ListaService listaService;

    public ListaController(ListaService listaService) {
        this.listaService = listaService;
    }

    @PostMapping("/usuario/{usuarioId}")
    @Operation(
            summary = "Crear una nueva lista",
            description = "Crea una nueva lista personalizada para un usuario",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Lista creada correctamente"),
                    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
            }
    )
    public ResponseEntity<AniverseResponse<ListaDTO>> createLista(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable Long usuarioId,

            @Parameter(description = "Datos de la lista a crear", required = true)
            @Valid @RequestBody ListaCreateDTO listaDTO) {

        try {
            ListaDTO createdLista = listaService.createLista(usuarioId, listaDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AniverseResponse.success("Lista creada con éxito", createdLista));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Usuario no encontrado"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AniverseResponse.error("No tienes permisos para realizar esta acción"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/usuario/{usuarioId}")
    @Operation(
            summary = "Obtener listas de un usuario",
            description = "Retorna todas las listas de un usuario específico",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Listas obtenidas correctamente"),
                    @ApiResponse(responseCode = "404", description = "Usuario no encontrado")
            }
    )
    public ResponseEntity<AniverseResponse<Page<ListaDTO>>> getListasByUsuario(
            @Parameter(description = "ID del usuario", required = true)
            @PathVariable Long usuarioId,

            @Parameter(description = "Número de página (empieza desde 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamaño de la página")
            @RequestParam(defaultValue = "10") int size) {

        try {
            Page<ListaDTO> listas = listaService.getListasByUsuario(
                    usuarioId, PageRequest.of(page, size));
            return ResponseEntity.ok(AniverseResponse.success("Listas obtenidas con éxito", listas));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Usuario no encontrado"));
        }
    }

    @GetMapping("/publicas")
    @Transactional(readOnly = true)
    @Operation(
            summary = "Obtener listas públicas",
            description = "Retorna todas las listas públicas de la plataforma",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Listas obtenidas correctamente")
            }
    )
    public ResponseEntity<AniverseResponse<Page<ListaDTO>>> getPublicLists(
            @Parameter(description = "Número de página (empieza desde 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamaño de la página")
            @RequestParam(defaultValue = "10") int size) {

        Page<ListaDTO> listas = listaService.getPublicLists(PageRequest.of(page, size));
        return ResponseEntity.ok(AniverseResponse.success("Listas públicas obtenidas con éxito", listas));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Obtener detalles de una lista",
            description = "Retorna los detalles de una lista específica",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                    @ApiResponse(responseCode = "404", description = "Lista no encontrada")
            }
    )
    public ResponseEntity<AniverseResponse<ListaDTO>> getListaDetalles(
            @Parameter(description = "ID de la lista", required = true)
            @PathVariable Long id) {

        try {
            ListaDTO lista = listaService.getListaDetalles(id);
            return ResponseEntity.ok(AniverseResponse.success("Lista obtenida con éxito", lista));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Lista no encontrada"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AniverseResponse.error("No tienes permisos para ver esta lista"));
        }
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Actualizar una lista",
            description = "Actualiza los datos de una lista existente",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista actualizada correctamente"),
                    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                    @ApiResponse(responseCode = "404", description = "Lista no encontrada")
            }
    )
    public ResponseEntity<AniverseResponse<ListaDTO>> updateLista(
            @Parameter(description = "ID de la lista", required = true)
            @PathVariable Long id,

            @Parameter(description = "Datos actualizados de la lista", required = true)
            @Valid @RequestBody ListaCreateDTO listaDTO) {

        try {
            ListaDTO updatedLista = listaService.updateLista(id, listaDTO);
            return ResponseEntity.ok(AniverseResponse.success("Lista actualizada con éxito", updatedLista));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Lista no encontrada"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AniverseResponse.error("No tienes permisos para actualizar esta lista"));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Eliminar una lista",
            description = "Elimina lógicamente una lista existente",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Lista eliminada correctamente"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                    @ApiResponse(responseCode = "404", description = "Lista no encontrada")
            }
    )
    public ResponseEntity<AniverseResponse<String>> deleteLista(
            @Parameter(description = "ID de la lista", required = true)
            @PathVariable Long id) {

        try {
            listaService.softDeleteLista(id);
            return ResponseEntity.ok(AniverseResponse.success("Lista eliminada con éxito"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Lista no encontrada"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AniverseResponse.error("No tienes permisos para eliminar esta lista"));
        }
    }

    @GetMapping("/{id}/animes")
    @Operation(
            summary = "Obtener animes de una lista",
            description = "Retorna todos los animes incluidos en una lista específica",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Animes obtenidos correctamente"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                    @ApiResponse(responseCode = "404", description = "Lista no encontrada")
            }
    )
    public ResponseEntity<AniverseResponse<List<ListaAnimeDTO>>> getAnimesFromLista(
            @Parameter(description = "ID de la lista", required = true)
            @PathVariable Long id) {

        try {
            List<ListaAnimeDTO> animes = listaService.getAnimesFromLista(id);
            return ResponseEntity.ok(AniverseResponse.success("Animes obtenidos con éxito", animes));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Lista no encontrada"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AniverseResponse.error("No tienes permisos para ver esta lista"));
        }
    }

    @PostMapping("/{id}/animes")
    @Operation(
            summary = "Añadir anime a una lista",
            description = "Añade un anime a una lista existente",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Anime añadido correctamente"),
                    @ApiResponse(responseCode = "400", description = "Datos inválidos o anime ya existe en la lista"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                    @ApiResponse(responseCode = "404", description = "Lista o anime no encontrado")
            }
    )
    public ResponseEntity<AniverseResponse<ListaAnimeDTO>> addAnimeToLista(
            @Parameter(description = "ID de la lista", required = true)
            @PathVariable Long id,

            @Parameter(description = "Datos del anime a añadir", required = true)
            @Valid @RequestBody ListaAnimeCreateDTO animeDTO) {

        try {
            ListaAnimeDTO addedAnime = listaService.addAnimeToLista(id, animeDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(AniverseResponse.success("Anime añadido a la lista con éxito", addedAnime));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Lista o anime no encontrado"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AniverseResponse.error("No tienes permisos para modificar esta lista"));
        }
    }

    @PutMapping("/{listaId}/animes/{animeId}")
    @Operation(
            summary = "Actualizar anime en una lista",
            description = "Actualiza los datos de un anime en una lista",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Anime actualizado correctamente"),
                    @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                    @ApiResponse(responseCode = "404", description = "Lista, anime o relación no encontrada")
            }
    )
    public ResponseEntity<AniverseResponse<ListaAnimeDTO>> updateAnimeInLista(
            @Parameter(description = "ID de la lista", required = true)
            @PathVariable Long listaId,

            @Parameter(description = "ID del anime", required = true)
            @PathVariable Long animeId,

            @Parameter(description = "Datos actualizados", required = true)
            @Valid @RequestBody ListaAnimeCreateDTO updateDTO) {

        try {
            ListaAnimeDTO updatedAnime = listaService.updateAnimeInLista(listaId, animeId, updateDTO);
            return ResponseEntity.ok(AniverseResponse.success("Anime actualizado en la lista con éxito", updatedAnime));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Lista, anime o relación no encontrada"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AniverseResponse.error("No tienes permisos para modificar esta lista"));
        }
    }

    @DeleteMapping("/{listaId}/animes/{animeId}")
    @Operation(
            summary = "Eliminar anime de una lista",
            description = "Elimina un anime de una lista",
            responses = {
                    @ApiResponse(responseCode = "204", description = "Anime eliminado correctamente"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                    @ApiResponse(responseCode = "404", description = "Lista, anime o relación no encontrada")
            }
    )
    public ResponseEntity<AniverseResponse<String>> removeAnimeFromLista(
            @Parameter(description = "ID de la lista", required = true)
            @PathVariable Long listaId,

            @Parameter(description = "ID del anime", required = true)
            @PathVariable Long animeId) {

        try {
            listaService.removeAnimeFromLista(listaId, animeId);
            return ResponseEntity.ok(AniverseResponse.success("Anime eliminado de la lista con éxito"));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Lista, anime o relación no encontrada"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AniverseResponse.error("No tienes permisos para modificar esta lista"));
        }
    }

    @GetMapping("/eliminadas")
    @Operation(
            summary = "Obtener listas eliminadas",
            description = "Retorna todas las listas que han sido eliminadas lógicamente",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Listas obtenidas correctamente")
            }
    )
    public ResponseEntity<AniverseResponse<Page<ListaDTO>>> getDeletedListas(
            @Parameter(description = "Número de página (empieza desde 0)")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Tamaño de la página")
            @RequestParam(defaultValue = "10") int size) {

        Page<ListaDTO> listas = listaService.getDeletedListas(PageRequest.of(page, size));
        return ResponseEntity.ok(AniverseResponse.success("Listas eliminadas obtenidas con éxito", listas));
    }

    @PostMapping("/{id}/restaurar")
    @Operation(
            summary = "Restaurar una lista eliminada",
            description = "Restaura una lista que ha sido eliminada lógicamente",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Lista restaurada correctamente"),
                    @ApiResponse(responseCode = "400", description = "La lista no está eliminada"),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado"),
                    @ApiResponse(responseCode = "404", description = "Lista no encontrada")
            }
    )
    public ResponseEntity<AniverseResponse<ListaDTO>> restoreLista(
            @Parameter(description = "ID de la lista", required = true)
            @PathVariable Long id) {

        try {
            ListaDTO restaurada = listaService.restoreLista(id);
            return ResponseEntity.ok(AniverseResponse.success("Lista restaurada con éxito", restaurada));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Lista no encontrada"));
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(AniverseResponse.error("No tienes permisos para restaurar esta lista"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }
}