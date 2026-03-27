package com.aniverse.backend.service;

import com.aniverse.backend.model.Comentario;
import com.aniverse.backend.model.Resenya;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.repository.ComentarioRepository;
import com.aniverse.backend.repository.ResenyaRepository;
import com.aniverse.backend.repository.UsuarioRepository;
import com.aniverse.backend.util.NotificacionTipo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final UsuarioRepository usuarioRepository;
    private final ResenyaRepository resenyaRepository;
    private final NotificacionService notificacionService;

    public ComentarioService(ComentarioRepository comentarioRepository,
                             UsuarioRepository usuarioRepository,
                             ResenyaRepository resenyaRepository,
                             NotificacionService notificacionService) {
        this.comentarioRepository = comentarioRepository;
        this.usuarioRepository = usuarioRepository;
        this.resenyaRepository = resenyaRepository;
        this.notificacionService = notificacionService;
    }

    @Transactional
    public Comentario saveComentario(Long usuarioId, Long resenyaId, String contenido) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new NoSuchElementException("Usuario no encontrado con ID: " + usuarioId));

        Resenya resenya = resenyaRepository.findById(resenyaId)
                .orElseThrow(() -> new NoSuchElementException("Reseña no encontrada con ID: " + resenyaId));

        Comentario comentario = new Comentario();
        comentario.setUsuario(usuario);
        comentario.setResenya(resenya);
        comentario.setContenido(contenido);

        Comentario guardado = comentarioRepository.save(comentario);

        // Generar notificación al autor de la reseña (si no es el mismo usuario)
        if (!resenya.getUsuario().getId().equals(usuarioId)) {
            notificacionService.crearNotificacion(
                    resenya.getUsuario().getId(),  // destinatario (autor de la reseña)
                    usuarioId,                     // emisor (autor del comentario)
                    NotificacionTipo.COMENTARIO,   // tipo
                    usuario.getNombre() + " ha comentado en tu reseña de " + resenya.getAnime().getTitulo(), // mensaje
                    comentario.getId(),            // objetoId (ID del comentario)
                    "COMENTARIO",                  // objetoTipo
                    "/anime/" + resenya.getAnime().getId() + "/resenas/" + resenya.getId() // URL
            );
        }

        return guardado;
    }

    public List<Comentario> getComentariosByResenya(Long resenyaId) {
        Resenya resenya = resenyaRepository.findById(resenyaId)
                .orElseThrow(() -> new NoSuchElementException("Reseña no encontrada con ID: " + resenyaId));

        return comentarioRepository.findByResenya(resenya);
    }

    public void deleteComentario(Long id) {
        if (!comentarioRepository.existsById(id)) {
            throw new NoSuchElementException("Comentario no encontrado con ID: " + id);
        }
        comentarioRepository.deleteById(id);
    }
}
