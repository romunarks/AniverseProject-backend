package com.aniverse.backend.controller;

import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.model.Comentario;
import com.aniverse.backend.service.ComentarioService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/comentarios")
public class ComentarioController {

    private final ComentarioService comentarioService;

    public ComentarioController(ComentarioService comentarioService) {
        this.comentarioService = comentarioService;
    }

    @PostMapping
    public ResponseEntity<AniverseResponse<Map<String, Object>>> saveComentario(
            @RequestParam Long usuarioId,
            @RequestParam Long resenyaId,
            @RequestParam String contenido) {
        try {
            Comentario comentario = comentarioService.saveComentario(usuarioId, resenyaId, contenido);

            // Crear un DTO simple para la respuesta
            Map<String, Object> response = new HashMap<>();
            response.put("id", comentario.getId());
            response.put("contenido", comentario.getContenido());
            response.put("fecha", comentario.getFecha());
            response.put("usuarioId", comentario.getUsuario().getId());
            response.put("usuarioNombre", comentario.getUsuario().getNombre());
            response.put("resenyaId", comentario.getResenya().getId());

            return ResponseEntity.ok(AniverseResponse.success("Comentario guardado con éxito", response));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error(e.getMessage()));
        }
    }

    @GetMapping("/resenya/{resenyaId}")
    public ResponseEntity<AniverseResponse<List<Comentario>>> getComentariosByResenya(@PathVariable Long resenyaId) {
        try {
            List<Comentario> comentarios = comentarioService.getComentariosByResenya(resenyaId);
            return ResponseEntity.ok(AniverseResponse.success("Comentarios obtenidos con éxito", comentarios));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al obtener comentarios"));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<AniverseResponse<String>> deleteComentario(@PathVariable Long id) {
        try {
            comentarioService.deleteComentario(id);
            return ResponseEntity.ok(AniverseResponse.success("Comentario eliminado con éxito"));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Comentario no encontrado"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AniverseResponse.error("Error al eliminar el comentario"));
        }
    }
}