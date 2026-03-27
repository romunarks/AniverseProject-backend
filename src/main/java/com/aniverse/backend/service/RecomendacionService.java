package com.aniverse.backend.service;

import com.aniverse.backend.dto.AnimeDTO;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Favorito;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.model.Votacion;
import com.aniverse.backend.repository.AnimeRepository;
import com.aniverse.backend.repository.FavoritoRepository;
import com.aniverse.backend.repository.VotacionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects; // ✅ Agregar esta línea al principio del archivo

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecomendacionService {

    private final AnimeRepository animeRepository;
    private final FavoritoRepository favoritoRepository;
    private final VotacionRepository votacionRepository;
    private final UsuarioService usuarioService;

    public RecomendacionService(
            AnimeRepository animeRepository,
            FavoritoRepository favoritoRepository,
            VotacionRepository votacionRepository,
            UsuarioService usuarioService) {
        this.animeRepository = animeRepository;
        this.favoritoRepository = favoritoRepository;
        this.votacionRepository = votacionRepository;
        this.usuarioService = usuarioService;
    }

    /**
     * Recomienda animes basados en los favoritos y votaciones del usuario
     */
    @Transactional(readOnly = true)
    public List<AnimeDTO> getRecomendacionesPersonalizadas(Long usuarioId, int limit) {
        System.out.println("🔍 DEBUG: ===== INICIANDO RECOMENDACIONES =====");
        System.out.println("🔍 DEBUG: Usuario ID: " + usuarioId + ", Limit: " + limit);

        Usuario usuario = usuarioService.getUsuarioEntityById(usuarioId);
        System.out.println("🔍 DEBUG: Usuario encontrado: " + (usuario != null ? usuario.getNombre() : "null"));

        // Obtener géneros favoritos del usuario - MEJORADO PARA MANEJAR GÉNEROS MÚLTIPLES
        Map<String, Integer> generosPuntuados = new HashMap<>();

        // Contar géneros en favoritos
        List<Favorito> favoritos = favoritoRepository.findByUsuario(usuario);
        System.out.println("🔍 DEBUG: Favoritos encontrados: " + favoritos.size());

        for (Favorito favorito : favoritos) {
            System.out.println("🔍 DEBUG: Favorito - Anime: " + favorito.getAnime().getTitulo() +
                    ", Género: " + favorito.getAnime().getGenero());
            String genero = favorito.getAnime().getGenero();
            if (genero != null && !genero.isEmpty()) {
                // ✅ SEPARAR GÉNEROS MÚLTIPLES POR COMAS
                String[] generos = genero.split(",");
                for (String g : generos) {
                    String generoLimpio = g.trim();
                    if (!generoLimpio.isEmpty()) {
                        generosPuntuados.put(generoLimpio, generosPuntuados.getOrDefault(generoLimpio, 0) + 2);
                    }
                }
            }
        }

        // Agregar géneros votados con alta puntuación (≥ 4.0)
        List<Votacion> votaciones = votacionRepository.findByUsuario(usuario);
        System.out.println("🔍 DEBUG: Votaciones encontradas: " + votaciones.size());

        for (Votacion votacion : votaciones) {
            if (votacion.getPuntuacion() >= 4.0) {
                System.out.println("🔍 DEBUG: Votación alta - Anime: " + votacion.getAnime().getTitulo() +
                        ", Género: " + votacion.getAnime().getGenero() +
                        ", Puntuación: " + votacion.getPuntuacion());
                String genero = votacion.getAnime().getGenero();
                if (genero != null && !genero.isEmpty()) {
                    // ✅ SEPARAR GÉNEROS MÚLTIPLES POR COMAS
                    String[] generos = genero.split(",");
                    for (String g : generos) {
                        String generoLimpio = g.trim();
                        if (!generoLimpio.isEmpty()) {
                            generosPuntuados.put(generoLimpio, generosPuntuados.getOrDefault(generoLimpio, 0) + 1);
                        }
                    }
                }
            }
        }

        System.out.println("🔍 DEBUG: Géneros puntuados (separados): " + generosPuntuados);

        // IDs de animes que el usuario ya ha visto (favoritos o votados)
        Set<Long> animesVistos = new HashSet<>();
        favoritos.forEach(f -> animesVistos.add(f.getAnime().getId()));
        votaciones.forEach(v -> animesVistos.add(v.getAnime().getId()));

        System.out.println("🔍 DEBUG: Animes ya vistos: " + animesVistos.size());

        List<AnimeDTO> recomendacionesFiltradas = new ArrayList<>();

        // ✅ ESTRATEGIA MÚLTIPLE: PROBAR VARIOS GÉNEROS
        if (!generosPuntuados.isEmpty()) {
            // Obtener top 3 géneros preferidos
            List<String> topGeneros = generosPuntuados.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            System.out.println("🔍 DEBUG: Top 3 géneros preferidos: " + topGeneros);

            // Probar cada género hasta obtener suficientes recomendaciones
            for (String genero : topGeneros) {
                if (recomendacionesFiltradas.size() >= limit) break;

                System.out.println("🔍 DEBUG: Probando género: " + genero);
                List<AnimeDTO> recomendacionesGenero = getTopRatedAnimesByGenre(genero, limit * 2);
                System.out.println("🔍 DEBUG: Encontrados para " + genero + ": " + recomendacionesGenero.size());

                // Filtrar animes ya vistos y duplicados
                List<AnimeDTO> nuevas = recomendacionesGenero.stream()
                        .filter(anime -> anime.getId() == null || !animesVistos.contains(anime.getId()))
                        .filter(anime -> recomendacionesFiltradas.stream()
                                .noneMatch(existing -> Objects.equals(existing.getId(), anime.getId())))
                        .collect(Collectors.toList());

                recomendacionesFiltradas.addAll(nuevas);
                System.out.println("🔍 DEBUG: Total acumulado: " + recomendacionesFiltradas.size());
            }
        }

        // ✅ SI AÚN NO HAY SUFICIENTES, AGREGAR TOP RATED GENERALES
        if (recomendacionesFiltradas.size() < limit) {
            System.out.println("🔍 DEBUG: Insuficientes (" + recomendacionesFiltradas.size() +
                    "), agregando top rated generales...");

            List<AnimeDTO> topGeneral = getTopRatedAnimesByGenre(null, limit * 3);
            System.out.println("🔍 DEBUG: Top generales encontrados: " + topGeneral.size());

            List<AnimeDTO> extras = topGeneral.stream()
                    .filter(anime -> anime.getId() == null || !animesVistos.contains(anime.getId()))
                    .filter(anime -> recomendacionesFiltradas.stream()
                            .noneMatch(existing -> Objects.equals(existing.getId(), anime.getId())))
                    .limit(limit - recomendacionesFiltradas.size())
                    .collect(Collectors.toList());

            recomendacionesFiltradas.addAll(extras);
            System.out.println("🔍 DEBUG: Después de agregar generales: " + recomendacionesFiltradas.size());
        }

        // ✅ ÚLTIMO RECURSO: INCLUIR ALGUNOS YA VISTOS PERO ALTAMENTE PUNTUADOS
        if (recomendacionesFiltradas.size() < Math.min(3, limit)) {
            System.out.println("🔍 DEBUG: Muy pocas recomendaciones, incluyendo algunos ya vistos...");

            List<AnimeDTO> relleno = getTopRatedAnimesByGenre(null, limit * 2).stream()
                    .filter(anime -> recomendacionesFiltradas.stream()
                            .noneMatch(existing -> Objects.equals(existing.getId(), anime.getId())))
                    .limit(limit - recomendacionesFiltradas.size())
                    .collect(Collectors.toList());

            recomendacionesFiltradas.addAll(relleno);
            System.out.println("🔍 DEBUG: Recomendaciones finales con relleno: " + recomendacionesFiltradas.size());
        }

        // Limitar a la cantidad solicitada
        List<AnimeDTO> resultado = recomendacionesFiltradas.stream()
                .limit(limit)
                .collect(Collectors.toList());

        System.out.println("🔍 DEBUG: Resultado final: " + resultado.size() + " recomendaciones");
        System.out.println("🔍 DEBUG: ===== FIN RECOMENDACIONES =====");

        return resultado;
    }
    /**
     * Obtiene los animes mejor puntuados por género
     */
    @Transactional(readOnly = true)
    public List<AnimeDTO> getTopRatedAnimesByGenre(String genero, int limit) {
        List<Anime> animes = new ArrayList<>();

        if (genero == null || genero.isEmpty()) {
            // Si no se especifica género, obtener los mejor puntuados generales
            animes = animeRepository.findTopRatedAnimes(
                    org.springframework.data.domain.PageRequest.of(0, limit));
        } else {
            // Consulta específica para género
            // Esta consulta es hipotética - deberías implementarla en AnimeRepository
            animes = animeRepository.findTopRatedAnimesByGenre(
                    genero, org.springframework.data.domain.PageRequest.of(0, limit));
        }

        return animes.stream()
                .map(anime -> new AnimeDTO(
                        anime.getId(),
                        anime.getTitulo(),
                        anime.getDescripcion(),
                        anime.getGenero(),
                        anime.getImagenUrl()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Recomienda animes similares a uno específico
     */
    @Transactional(readOnly = true)
    public List<AnimeDTO> getAnimesSimilares(Long animeId, int limit) {
        Anime anime = animeRepository.findByIdAndEliminadoFalse(animeId)
                .orElseThrow(() -> new RuntimeException("Anime no encontrado"));

        // Buscar animes del mismo género
        String genero = anime.getGenero();
        if (genero == null || genero.isEmpty()) {
            return getTopRatedAnimesByGenre(null, limit);
        }

        // Implementación simple: mismo género, ordenados por puntuación
        List<Anime> animesSimilares = animeRepository.findTopRatedAnimesByGenre(
                genero, org.springframework.data.domain.PageRequest.of(0, limit + 1));

        // Filtrar el anime actual y limitar resultados
        return animesSimilares.stream()
                .filter(a -> !a.getId().equals(animeId))
                .limit(limit)
                .map(a -> new AnimeDTO(
                        a.getId(),
                        a.getTitulo(),
                        a.getDescripcion(),
                        a.getGenero(),
                        a.getImagenUrl()
                ))
                .collect(Collectors.toList());
    }
}