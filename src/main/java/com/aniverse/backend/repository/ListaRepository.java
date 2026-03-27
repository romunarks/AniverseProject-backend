package com.aniverse.backend.repository;

import com.aniverse.backend.model.Lista;
import com.aniverse.backend.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ListaRepository extends JpaRepository<Lista, Long> {
    List<Lista> findByUsuario(Usuario usuario);

    Page<Lista> findByUsuario(Usuario usuario, Pageable pageable);

    @Query("SELECT l FROM Lista l WHERE l.publica = true ORDER BY l.createdAt DESC")
    Page<Lista> findPublicLists(Pageable pageable);

    @Query("SELECT l FROM Lista l WHERE l.publica = true AND " +
            "(LOWER(l.nombre) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
            "LOWER(l.descripcion) LIKE LOWER(CONCAT('%', :query, '%')))")
    Page<Lista> searchPublicLists(@Param("query") String query, Pageable pageable);

    // Métodos para soft delete
    @Query("SELECT l FROM Lista l WHERE l.eliminado = false")
    List<Lista> findAllActive();

    @Query("SELECT l FROM Lista l WHERE l.usuario = :usuario AND l.eliminado = false")
    List<Lista> findByUsuarioAndEliminadoFalse(@Param("usuario") Usuario usuario);

    @Query("SELECT l FROM Lista l WHERE l.usuario = :usuario AND l.eliminado = false")
    Page<Lista> findByUsuarioAndEliminadoFalse(@Param("usuario") Usuario usuario, Pageable pageable);

    @Query("SELECT l FROM Lista l WHERE l.id = :id AND l.eliminado = false")
    Optional<Lista> findByIdAndEliminadoFalse(@Param("id") Long id);

    @Query("SELECT l FROM Lista l WHERE l.publica = true AND l.eliminado = false ORDER BY l.createdAt DESC")
    Page<Lista> findPublicListsActive(Pageable pageable);

    @Query("SELECT l FROM Lista l WHERE l.eliminado = true")
    Page<Lista> findAllDeleted(Pageable pageable);

    // Nuevo método para contar listas no eliminadas por usuarioId
    @Query("SELECT COUNT(l) FROM Lista l WHERE l.usuario.id = :usuarioId AND l.eliminado = false")
    long countByUsuarioIdAndEliminadoFalse(@Param("usuarioId") Long usuarioId);

}