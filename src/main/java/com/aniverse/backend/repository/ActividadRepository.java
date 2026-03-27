package com.aniverse.backend.repository;

import com.aniverse.backend.model.Actividad;
import com.aniverse.backend.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActividadRepository extends JpaRepository<Actividad, Long> {

  // Obtener actividades de un usuario
  Page<Actividad> findByUsuarioOrderByFechaDesc(Usuario usuario, Pageable pageable);

  // Obtener actividades de usuarios que sigue un usuario
  @Query("SELECT a FROM Actividad a WHERE a.usuario IN " +
          "(SELECT s.seguido FROM Seguidor s WHERE s.seguidor = :usuario) " +
          "ORDER BY a.fecha DESC")
  Page<Actividad> findFeedActivities(@Param("usuario") Usuario usuario, Pageable pageable);

  // Obtener actividades relacionadas con un anime específico
  @Query("SELECT a FROM Actividad a WHERE a.objetoTipo = 'ANIME' AND a.objetoId = :animeId ORDER BY a.fecha DESC")
  Page<Actividad> findByAnime(@Param("animeId") Long animeId, Pageable pageable);

  // Eliminar actividades antiguas
  void deleteByFechaBefore(LocalDateTime fecha);
}