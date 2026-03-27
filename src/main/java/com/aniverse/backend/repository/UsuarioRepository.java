package com.aniverse.backend.repository;

import com.aniverse.backend.dto.UsuarioDTO;
import com.aniverse.backend.model.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    Optional<Usuario> findByEmail(String email); // Buscar usuario por email
    boolean existsByEmail(String email); // Verificar si un email ya está en uso

    @Query("SELECT u FROM Usuario u WHERE u.eliminado = false")
    Page<Usuario> findAllActive(Pageable pageable);

    @Query("SELECT u FROM Usuario u WHERE u.id = :id AND u.eliminado = false")
    Optional<Usuario> findByIdAndEliminadoFalse(@Param("id") Long id);

    @Query("SELECT u FROM Usuario u WHERE u.email = :email AND u.eliminado = false")
    Optional<Usuario> findByEmailAndEliminadoFalse(@Param("email") String email);

    @Query("SELECT u FROM Usuario u WHERE u.eliminado = true")
    Page<Usuario> findAllDeleted(Pageable pageable);

    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM Usuario u WHERE u.email = :email AND u.eliminado = false")
    boolean existsByEmailAndEliminadoFalse(@Param("email") String email);

    // ELIMINADO: UsuarioDTO getUsuarioByEmail(String email);
    // Este método causaba el error de Hibernate. Ahora usaremos el service layer.


}