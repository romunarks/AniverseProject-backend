package com.aniverse.backend.controller;

import com.aniverse.backend.dto.response.AniverseResponse;
import com.aniverse.backend.service.ImagenService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/imagenes")
@Tag(name = "Imágenes", description = "API para gestionar imágenes")
public class ImagenController {

    private final ImagenService imagenService;
    private final String uploadDir;

    public ImagenController(ImagenService imagenService,
                            @Value("${app.upload.dir:${user.home}/uploads/aniverse}") String uploadDir) {
        this.imagenService = imagenService;
        this.uploadDir = uploadDir;
    }

    @PostMapping("/upload")
    @Operation(
            summary = "Subir una imagen",
            description = "Sube una imagen al servidor y devuelve su URL",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Imagen subida correctamente"),
                    @ApiResponse(responseCode = "400", description = "Error al subir la imagen")
            }
    )
    public ResponseEntity<AniverseResponse<Map<String, String>>> subirImagen(
            @Parameter(description = "Archivo de imagen a subir", required = true)
            @RequestParam("file") MultipartFile file) {
        try {
            String imagenUrl = imagenService.guardarImagen(file);

            Map<String, String> response = new HashMap<>();
            response.put("url", imagenUrl);

            return ResponseEntity.ok(
                    AniverseResponse.success("Imagen subida correctamente", response));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(AniverseResponse.error("Error al subir la imagen: " + e.getMessage()));
        }
    }

    // Note: Este endpoint devuelve directamente el recurso de imagen, no necesita AniverseResponse
    @GetMapping("/{filename:.+}")
    @Operation(
            summary = "Obtener una imagen",
            description = "Obtiene una imagen por su nombre de archivo",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Imagen obtenida correctamente"),
                    @ApiResponse(responseCode = "404", description = "Imagen no encontrada")
            }
    )
    public ResponseEntity<Resource> obtenerImagen(
            @Parameter(description = "Nombre del archivo de imagen", required = true)
            @PathVariable String filename) {
        try {
            Path filePath = Paths.get(uploadDir).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists()) {
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                        .contentType(MediaType.IMAGE_JPEG) // Ajustar según el tipo de imagen
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{filename:.+}")
    @Operation(
            summary = "Eliminar una imagen",
            description = "Elimina una imagen por su nombre de archivo",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Imagen eliminada correctamente"),
                    @ApiResponse(responseCode = "404", description = "Imagen no encontrada")
            }
    )
    public ResponseEntity<AniverseResponse<String>> eliminarImagen(
            @Parameter(description = "Nombre del archivo de imagen", required = true)
            @PathVariable String filename) {
        try {
            String rutaImagen = "/images/" + filename;
            imagenService.eliminarImagen(rutaImagen);
            return ResponseEntity.ok(
                    AniverseResponse.success("Imagen eliminada correctamente"));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(AniverseResponse.error("Imagen no encontrada"));
        }
    }
}