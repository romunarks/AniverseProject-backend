package com.aniverse.backend.repository;

import com.aniverse.backend.model.Comentario;
import com.aniverse.backend.model.Resenya;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComentarioRepository extends JpaRepository<Comentario, Long> {
  List<Comentario> findByResenya(Resenya resenya); // Obtener comentarios de una reseña
}
