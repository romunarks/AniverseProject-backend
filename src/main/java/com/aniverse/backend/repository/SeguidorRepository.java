package com.aniverse.backend.repository;

import com.aniverse.backend.model.Seguidor;
import com.aniverse.backend.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SeguidorRepository extends JpaRepository<Seguidor, Long> {

    // Buscar relación de seguimiento entre dos usuarios
    Optional<Seguidor> findBySeguidorAndSeguido(Usuario seguidor, Usuario seguido);

    // Comprobar si existe relación de seguimiento
    boolean existsBySeguidorAndSeguido(Usuario seguidor, Usuario seguido);

    // Obtener todos los usuarios a los que sigue un usuario
    Page<Seguidor> findBySeguidor(Usuario seguidor, Pageable pageable);

    // Obtener todos los seguidores de un usuario
    Page<Seguidor> findBySeguido(Usuario seguido, Pageable pageable);

    // Contar cuántos usuarios sigue un usuario
    long countBySeguidor(Usuario seguidor);

    // Contar cuántos seguidores tiene un usuario
    long countBySeguido(Usuario seguido);

    // Buscar usuarios que siguen a un usuario específico y también son seguidos por él
    @Query("SELECT s.seguidor FROM Seguidor s WHERE s.seguido = :usuario AND " +
            "EXISTS (SELECT 1 FROM Seguidor s2 WHERE s2.seguidor = :usuario AND s2.seguido = s.seguidor)")
    Page<Usuario> findMutualFollowers(@Param("usuario") Usuario usuario, Pageable pageable);
}