package com.aniverse.backend.service;

import com.aniverse.backend.dto.UsuarioStatsDTO;
import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Favorito;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.model.Votacion;
import com.aniverse.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UsuarioStatsService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioStatsService.class);

    private final UsuarioRepository usuarioRepository;
    private final FavoritoRepository favoritoRepository;
    private final ResenyaRepository resenyaRepository;
    private final ListaRepository listaRepository;
    private final VotacionRepository votacionRepository;
    private final SeguidorRepository seguidorRepository;

    public UsuarioStatsService(UsuarioRepository usuarioRepository,
                               FavoritoRepository favoritoRepository,
                               ResenyaRepository resenyaRepository,
                               ListaRepository listaRepository,
                               VotacionRepository votacionRepository,
                               SeguidorRepository seguidorRepository) {
        this.usuarioRepository = usuarioRepository;
        this.favoritoRepository = favoritoRepository;
        this.resenyaRepository = resenyaRepository;
        this.listaRepository = listaRepository;
        this.votacionRepository = votacionRepository;
        this.seguidorRepository = seguidorRepository;
    }

    @Transactional(readOnly = true)
    public UsuarioStatsDTO getUsuarioStats(Long usuarioId) {
        // Verificar si existe el usuario
        Usuario usuario = usuarioRepository.findByIdAndEliminadoFalse(usuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", usuarioId));

        // Verificar permisos: solo el propio usuario o administradores
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = auth.getName();
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!usuario.getEmail().equals(currentUsername) && !isAdmin) {
            throw new AccessDeniedException("No tienes permiso para ver las estadísticas de este usuario");
        }

        // Crear DTO base
        UsuarioStatsDTO stats = new UsuarioStatsDTO(usuarioId, usuario.getNombre());

        // Contar entidades relacionadas
        stats.setTotalFavoritos(countFavoritosByUsuario(usuarioId));
        stats.setTotalResenyas(countResenyasByUsuario(usuarioId));
        stats.setTotalListas(countListasByUsuario(usuarioId));
        stats.setTotalVotaciones(countVotacionesByUsuario(usuarioId));

        // Conteo de seguidores y seguidos
        stats.setTotalSeguidores(seguidorRepository.countBySeguido(usuario));
        stats.setTotalSiguiendo(seguidorRepository.countBySeguidor(usuario));

        // Calcular géneros preferidos basado en favoritos y votaciones altas
        Map<String, Integer> generosPreferidos = calcularGenerosPreferidos(usuario);
        stats.setGenerosPreferidos(generosPreferidos);

        // Calcular puntuación promedio dada por el usuario
        stats.setPuntuacionPromedio(calcularPuntuacionPromedio(usuario));

        return stats;
    }

    private Long countFavoritosByUsuario(Long usuarioId) {
        try {
            return favoritoRepository.countByUsuarioId(usuarioId);
        } catch (Exception e) {
            log.warn("Error contando favoritos para usuario {}: {}", usuarioId, e.getMessage());
            return 0L;
        }
    }

    private Long countResenyasByUsuario(Long usuarioId) {
        try {
            return resenyaRepository.countByUsuarioIdAndEliminadoFalse(usuarioId);
        } catch (Exception e) {
            log.warn("Error contando reseñas para usuario {}: {}", usuarioId, e.getMessage());
            return 0L;
        }
    }

    private Long countListasByUsuario(Long usuarioId) {
        try {
            return listaRepository.countByUsuarioIdAndEliminadoFalse(usuarioId);
        } catch (Exception e) {
            log.warn("Error contando listas para usuario {}: {}", usuarioId, e.getMessage());
            return 0L;
        }
    }

    private Long countVotacionesByUsuario(Long usuarioId) {
        try {
            return votacionRepository.countByUsuarioId(usuarioId);
        } catch (Exception e) {
            log.warn("Error contando votaciones para usuario {}: {}", usuarioId, e.getMessage());
            return 0L;
        }
    }

    private Map<String, Integer> calcularGenerosPreferidos(Usuario usuario) {
        Map<String, Integer> generosPuntuacion = new HashMap<>();

        try {
            // Obtener favoritos
            List<Favorito> favoritos = favoritoRepository.findByUsuario(usuario);
            for (Favorito favorito : favoritos) {
                Anime anime = favorito.getAnime();
                if (anime != null && anime.getGenero() != null && !anime.getGenero().isEmpty()) {
                    // Dividir múltiples géneros si están separados por comas
                    String[] generos = anime.getGenero().split(",");
                    for (String genero : generos) {
                        String generoTrim = genero.trim();
                        // Asignar 2 puntos a los favoritos
                        generosPuntuacion.put(generoTrim, generosPuntuacion.getOrDefault(generoTrim, 0) + 2);
                    }
                }
            }

            // Obtener votaciones con puntuación alta (4 o más)
            List<Votacion> votaciones = votacionRepository.findByUsuario(usuario);
            for (Votacion votacion : votaciones) {
                if (votacion.getPuntuacion() >= 4.0) {
                    Anime anime = votacion.getAnime();
                    if (anime != null && anime.getGenero() != null && !anime.getGenero().isEmpty()) {
                        String[] generos = anime.getGenero().split(",");
                        for (String genero : generos) {
                            String generoTrim = genero.trim();
                            // Asignar 1 punto a votaciones altas
                            generosPuntuacion.put(generoTrim, generosPuntuacion.getOrDefault(generoTrim, 0) + 1);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error calculando géneros preferidos para usuario {}: {}", usuario.getId(), e.getMessage());
        }

        // Ordenar por puntuación y limitar a los top 5 géneros
        return generosPuntuacion.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(5)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }

    private Double calcularPuntuacionPromedio(Usuario usuario) {
        try {
            List<Votacion> votaciones = votacionRepository.findByUsuario(usuario);
            if (votaciones.isEmpty()) {
                return 0.0;
            }

            double sum = 0.0;
            for (Votacion votacion : votaciones) {
                sum += votacion.getPuntuacion();
            }

            return Math.round((sum / votaciones.size()) * 10.0) / 10.0; // Redondear a 1 decimal
        } catch (Exception e) {
            log.warn("Error calculando puntuación promedio para usuario {}: {}", usuario.getId(), e.getMessage());
            return 0.0;
        }
    }
}