package com.aniverse.backend.service;

import com.aniverse.backend.dto.UsuarioActualizarDTO;
import com.aniverse.backend.dto.UsuarioDTO;
import com.aniverse.backend.dto.UsuarioRegistroDTO;
import com.aniverse.backend.exception.DuplicateResourceException;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.repository.SeguidorRepository;
import com.aniverse.backend.repository.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static com.aniverse.backend.controller.UsuarioController.log;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder; // Para cifrar contraseñas
    private final SeguidorRepository seguidorRepository;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder, SeguidorRepository seguidorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.seguidorRepository = seguidorRepository;
    }

    /**
     * NUEVO MÉTODO: Obtiene un usuario por email y lo convierte a DTO.
     * Este método reemplaza la funcionalidad del repository que causaba problemas.
     *
     * @param email Email del usuario a buscar.
     * @return UsuarioDTO con los datos del usuario.
     * @throws ResourceNotFoundException si no se encuentra el usuario.
     */
    @Transactional(readOnly = true)
    public UsuarioDTO getUsuarioByEmail(String email) {
        Usuario usuario = usuarioRepository.findByEmailAndEliminadoFalse(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con email: " + email));

        UsuarioDTO dto = new UsuarioDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRoles()
        );

        // Añadir conteos de seguidores y seguidos
        long seguidoresCount = seguidorRepository.countBySeguido(usuario);
        long siguiendoCount = seguidorRepository.countBySeguidor(usuario);

        dto.setSeguidoresCount(seguidoresCount);
        dto.setSiguiendoCount(siguiendoCount);
        dto.setSiguiendoPorUsuarioActual(false); // Para /me, no aplica autoseguimiento

        return dto;
    }

    /**
     * Guarda un nuevo usuario en la base de datos.
     * Si no se especifican roles, se asigna el rol "USER" por defecto.
     *
     * @param usuarioDTO Datos del usuario.
     * @return Usuario registrado.
     */
    public Usuario saveUsuario(UsuarioRegistroDTO usuarioDTO) {
        if (usuarioRepository.existsByEmailAndEliminadoFalse(usuarioDTO.getEmail())) {
            throw new IllegalArgumentException("El correo ya está en uso.");
        }

        Usuario usuario = new Usuario();
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setEmail(usuarioDTO.getEmail());
        usuario.setContrasenya(passwordEncoder.encode(usuarioDTO.getContrasenya()));
        usuario.setRoles(List.of("USER"));
        usuario.setEliminado(false); // Asegurar que el usuario nuevo esté activo

        return usuarioRepository.save(usuario);
    }

    // En UsuarioService.java
    public Page<UsuarioDTO> getDeletedUsuarios(Pageable pageable) {
        return usuarioRepository.findAllDeleted(pageable)
                .map(usuario -> new UsuarioDTO(
                        usuario.getId(),
                        usuario.getNombre(),
                        usuario.getEmail(),
                        usuario.getRoles()
                ));
    }

    /**
     * Busca un usuario por su ID.
     *
     * @param id ID del usuario.
     * @return UsuarioDTO con los datos del usuario.
     */
    public UsuarioDTO getUsuarioById(Long id, Long usuarioActualId) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        UsuarioDTO dto = new UsuarioDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRoles()
        );

        // Añadir conteos de seguidores y seguidos
        long seguidoresCount = seguidorRepository.countBySeguido(usuario);
        long siguiendoCount = seguidorRepository.countBySeguidor(usuario);

        dto.setSeguidoresCount(seguidoresCount);
        dto.setSiguiendoCount(siguiendoCount);

        // Verificar si el usuario actual está siguiendo a este usuario
        if (usuarioActualId != null && !usuarioActualId.equals(id)) {
            Usuario usuarioActual = usuarioRepository.getReferenceById(usuarioActualId);
            boolean siguiendo = seguidorRepository.existsBySeguidorAndSeguido(usuarioActual, usuario);
            dto.setSiguiendoPorUsuarioActual(siguiendo);
        } else {
            dto.setSiguiendoPorUsuarioActual(false);
        }

        return dto;
    }

    // Método original con un solo parámetro
    public UsuarioDTO getUsuarioById(Long id) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        return new UsuarioDTO(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getRoles()
        );
    }

    /**
     * Obtiene todos los usuarios en formato DTO.
     *
     * @return Lista de usuarios (DTO).
     */
    // Actualizar método getAllUsuarios
    public List<UsuarioDTO> getAllUsuarios() {
        return usuarioRepository.findAllActive(Pageable.unpaged())
                .stream()
                .map(usuario -> new UsuarioDTO(
                        usuario.getId(),
                        usuario.getNombre(),
                        usuario.getEmail(),
                        usuario.getRoles()
                ))
                .collect(Collectors.toList());
    }

    // Nuevo método con paginación
    public Page<UsuarioDTO> getAllUsuarios(Pageable pageable) {
        return usuarioRepository.findAllActive(pageable)
                .map(usuario -> new UsuarioDTO(
                        usuario.getId(),
                        usuario.getNombre(),
                        usuario.getEmail(),
                        usuario.getRoles()
                ));
    }

    /**
     * Elimina un usuario por su ID.
     *
     * @param id ID del usuario.
     */
    // Modificar método deleteUsuario para usar soft delete
    @Transactional
    public void softDeleteUsuario(Long id) {
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        usuario.setEliminado(true);
        usuario.setFechaEliminacion(LocalDateTime.now());

        // También marcar como eliminados los recursos asociados del usuario
        // Por ejemplo, sus listas, reseñas, etc.

        usuarioRepository.save(usuario);
    }

    /**
     * Comprueba si un email ya está registrado en la base de datos.
     *
     * @param email Email a verificar.
     * @return `true` si el email ya está en uso, `false` en caso contrario.
     */
    // Actualizar método existsByEmail
    public boolean existsByEmail(String email) {
        return usuarioRepository.existsByEmailAndEliminadoFalse(email);
    }

    /**
     * Autentica un usuario verificando sus credenciales.
     *
     * @param email Email del usuario.
     * @param contrasenya Contraseña sin cifrar.
     * @return Usuario autenticado o `null` si las credenciales son incorrectas.
     */
    // Actualizar método authenticate
    public Usuario authenticate(String email, String contrasenya) {
        return usuarioRepository.findByEmailAndEliminadoFalse(email)
                .filter(usuario -> passwordEncoder.matches(contrasenya, usuario.getContrasenya()))
                .orElse(null);
    }

    /**
     * Actualiza los datos de un usuario existente.
     *
     * @param id ID del usuario.
     *
     * @return Usuario actualizado.
     *
     */
    public UsuarioDTO updateUsuario(Long id, UsuarioActualizarDTO usuarioDTO) {
        Usuario usuarioExistente = usuarioRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        if (usuarioDTO.getNombre() != null && !usuarioDTO.getNombre().isEmpty()) {
            usuarioExistente.setNombre(usuarioDTO.getNombre());
        }

        if (usuarioDTO.getEmail() != null && usuarioDTO.getEmail().isEmpty()) {
            throw new IllegalArgumentException("El email no puede estar vacío.");
        }

        if (usuarioDTO.getEmail() != null && !usuarioDTO.getEmail().isEmpty()) {
            if (usuarioRepository.existsByEmailAndEliminadoFalse(usuarioDTO.getEmail()) &&
                    !usuarioExistente.getEmail().equals(usuarioDTO.getEmail())) {
                throw new IllegalArgumentException("El email ya está en uso.");
            }
            usuarioExistente.setEmail(usuarioDTO.getEmail());
        }

        usuarioRepository.save(usuarioExistente);

        return new UsuarioDTO(
                usuarioExistente.getId(),
                usuarioExistente.getNombre(),
                usuarioExistente.getEmail(),
                usuarioExistente.getRoles()
        );
    }

    public void updateContrasenya(Long id, String oldPassword, String newPassword) {
        if (newPassword == null || newPassword.isEmpty()) {
            throw new IllegalArgumentException("La nueva contraseña no puede estar vacía.");
        }

        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        if (!passwordEncoder.matches(oldPassword, usuario.getContrasenya())) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }

        usuario.setContrasenya(passwordEncoder.encode(newPassword));
        usuarioRepository.save(usuario);
    }

    // Añadir al UsuarioService
// Método para obtener la entidad Usuario (no el DTO)
    // Actualizar método getUsuarioEntityById
    public Usuario getUsuarioEntityById(Long id) {
        return usuarioRepository.findByIdAndEliminadoFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));
    }

    // Añadir método para restaurar usuario
    @Transactional
    public UsuarioDTO restoreUsuario(Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", id));

        if (!usuario.isEliminado()) {
            throw new IllegalStateException("El usuario no está eliminado, no se puede restaurar");
        }

        // Verificar si ya existe un usuario activo con el mismo email
        if (usuarioRepository.existsByEmailAndEliminadoFalse(usuario.getEmail())) {
            throw new DuplicateResourceException("Ya existe un usuario activo con el email: " + usuario.getEmail());
        }

        usuario.setEliminado(false);
        usuario.setFechaEliminacion(null);

        Usuario restaurado = usuarioRepository.save(usuario);

        return new UsuarioDTO(
                restaurado.getId(),
                restaurado.getNombre(),
                restaurado.getEmail(),
                restaurado.getRoles()
        );
    }

}