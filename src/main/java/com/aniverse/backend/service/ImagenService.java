package com.aniverse.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class ImagenService {

    @Value("${app.upload.dir:${user.home}/uploads/aniverse}")
    private String uploadDir;

    /**
     * Guarda una imagen en el sistema de archivos y retorna su URL relativa.
     *
     * @param archivo El archivo de imagen a guardar
     * @return URL relativa a la imagen guardada
     * @throws IOException Si ocurre un error al guardar la imagen
     * @throws IllegalArgumentException Si el archivo no es una imagen válida
     */
    public String guardarImagen(MultipartFile archivo) throws IOException {
        // Validar que el archivo existe y no está vacío
        if (archivo == null || archivo.isEmpty()) {
            throw new IllegalArgumentException("El archivo está vacío o no existe");
        }

        // Validar que el archivo es una imagen
        String contentType = archivo.getContentType();
        if (!contentType.startsWith("image/")) {
            throw new IllegalArgumentException("El archivo debe ser una imagen (JPEG, PNG, etc.)");
        }

        // Crear directorio si no existe
        Path directorioPath = Paths.get(uploadDir);
        if (!Files.exists(directorioPath)) {
            Files.createDirectories(directorioPath);
        }

        // Generar nombre único para la imagen
        String nombreOriginal = archivo.getOriginalFilename();
        String extension = "";
        if (nombreOriginal.contains(".")) {
            extension = nombreOriginal.substring(nombreOriginal.lastIndexOf("."));
        }
        String nombreArchivo = UUID.randomUUID().toString() + extension;

        // Guardar archivo
        Path rutaCompleta = directorioPath.resolve(nombreArchivo);
        Files.copy(archivo.getInputStream(), rutaCompleta);

        // Devolver la URL relativa
        return "/images/" + nombreArchivo;
    }

    /**
     * Elimina una imagen del sistema de archivos.
     *
     * @param rutaImagen URL relativa de la imagen a eliminar
     * @throws IOException Si ocurre un error al eliminar la imagen
     */
    public void eliminarImagen(String rutaImagen) throws IOException {
        if (rutaImagen != null && rutaImagen.startsWith("/images/")) {
            String nombreArchivo = rutaImagen.substring("/images/".length());
            Path rutaCompleta = Paths.get(uploadDir).resolve(nombreArchivo);

            if (Files.exists(rutaCompleta)) {
                Files.delete(rutaCompleta);
            }
        }
    }
}