package com.aniverse.backend.service;

import com.aniverse.backend.dto.AnimeDTO;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Favorito;
import com.aniverse.backend.model.Usuario;
import com.aniverse.backend.model.Votacion;
import com.aniverse.backend.repository.AnimeRepository;
import com.aniverse.backend.repository.FavoritoRepository;
import com.aniverse.backend.repository.UsuarioRepository;
import com.aniverse.backend.repository.VotacionRepository;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class AdvancedRecommendationService {

    private final AnimeRepository animeRepository;
    private final VotacionRepository votacionRepository;
    private final FavoritoRepository favoritoRepository;
    private final UsuarioRepository usuarioRepository;
    private final Cache<String, List<AnimeDTO>> recommendationCache;

    public AdvancedRecommendationService(AnimeRepository animeRepository,
                                         VotacionRepository votacionRepository,
                                         FavoritoRepository favoritoRepository,
                                         UsuarioRepository usuarioRepository) {
        this.animeRepository = animeRepository;
        this.votacionRepository = votacionRepository;
        this.favoritoRepository = favoritoRepository;
        this.usuarioRepository = usuarioRepository;
        this.recommendationCache = Caffeine.newBuilder()
                .maximumSize(1000)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
    }


    @Transactional(readOnly = true)
    @Async
    public CompletableFuture<List<AnimeDTO>> getPersonalizedRecommendations(Long usuarioId, int limit) {
        String cacheKey = "recommendations_" + usuarioId;

        List<AnimeDTO> cachedRecommendations = recommendationCache.getIfPresent(cacheKey);
        if (cachedRecommendations != null) {
            return CompletableFuture.completedFuture(cachedRecommendations);
        }

        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();

        // 1. Analizar preferencias del usuario
        Map<String, Double> genrePreferences = analyzeUserPreferences(usuario);

        // 2. Obtener usuarios similares (collaborative filtering)
        List<Usuario> similarUsers = findSimilarUsers(usuario);

        // 3. Content-based filtering
        List<AnimeDTO> contentBasedRecommendations = getContentBasedRecommendations(genrePreferences, limit);

        // 4. Collaborative filtering recommendations
        List<AnimeDTO> collaborativeRecommendations = getCollaborativeRecommendations(similarUsers, limit);

        // 5. Combinar y rankear
        List<AnimeDTO> finalRecommendations = combineAndRankRecommendations(
                contentBasedRecommendations,
                collaborativeRecommendations,
                genrePreferences,
                limit
        );

        recommendationCache.put(cacheKey, finalRecommendations);

        return CompletableFuture.completedFuture(finalRecommendations);
    }

    private Map<String, Double> analyzeUserPreferences(Usuario usuario) {
        Map<String, Double> genrePreferences = new HashMap<>();

        // Analizar favoritos
        List<Favorito> favoritos = favoritoRepository.findByUsuario(usuario);
        for (Favorito favorito : favoritos) {
            String genre = favorito.getAnime().getGenero();
            if (genre != null) {
                genrePreferences.merge(genre, 1.0, Double::sum);
            }
        }

        // Analizar votaciones altas (>= 4.0)
        List<Votacion> votaciones = votacionRepository.findByUsuario(usuario);
        for (Votacion votacion : votaciones) {
            if (votacion.getPuntuacion() >= 4.0) {
                String genre = votacion.getAnime().getGenero();
                if (genre != null) {
                    genrePreferences.merge(genre, 0.5, Double::sum);
                }
            }
        }

        // Normalizar
        double total = genrePreferences.values().stream().mapToDouble(v -> v).sum();
        if (total > 0) {
            genrePreferences.replaceAll((k, v) -> v / total);
        }

        return genrePreferences;
    }

    private List<Usuario> findSimilarUsers(Usuario usuario) {
        // Implementar algoritmo de similitud basado en favoritos y votaciones
        // Versión simplificada: usuarios con animes favoritos en común

        List<Favorito> userFavorites = favoritoRepository.findByUsuario(usuario);
        Set<Long> userAnimeIds = userFavorites.stream()
                .map(f -> f.getAnime().getId())
                .collect(Collectors.toSet());

        List<Usuario> allUsers = usuarioRepository.findAll();
        List<Usuario> similarUsers = new ArrayList<>();

        for (Usuario otherUser : allUsers) {
            if (otherUser.getId().equals(usuario.getId())) continue;

            List<Favorito> otherFavorites = favoritoRepository.findByUsuario(otherUser);
            Set<Long> otherAnimeIds = otherFavorites.stream()
                    .map(f -> f.getAnime().getId())
                    .collect(Collectors.toSet());

            // Calcular similitud (Jaccard)
            Set<Long> intersection = new HashSet<>(userAnimeIds);
            intersection.retainAll(otherAnimeIds);

            Set<Long> union = new HashSet<>(userAnimeIds);
            union.addAll(otherAnimeIds);

            double similarity = (double) intersection.size() / union.size();

            if (similarity > 0.2) { // umbral de similitud
                similarUsers.add(otherUser);
            }
        }

        // Ordenar por similitud
        similarUsers.sort((u1, u2) -> {
            double sim1 = calculateUserSimilarity(usuario, u1);
            double sim2 = calculateUserSimilarity(usuario, u2);
            return Double.compare(sim2, sim1);
        });

        return similarUsers.subList(0, Math.min(5, similarUsers.size()));
    }

    private double calculateUserSimilarity(Usuario user1, Usuario user2) {
        List<Favorito> favorites1 = favoritoRepository.findByUsuario(user1);
        List<Favorito> favorites2 = favoritoRepository.findByUsuario(user2);

        Set<Long> animeIds1 = favorites1.stream()
                .map(f -> f.getAnime().getId())
                .collect(Collectors.toSet());

        Set<Long> animeIds2 = favorites2.stream()
                .map(f -> f.getAnime().getId())
                .collect(Collectors.toSet());

        Set<Long> intersection = new HashSet<>(animeIds1);
        intersection.retainAll(animeIds2);

        Set<Long> union = new HashSet<>(animeIds1);
        union.addAll(animeIds2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private List<AnimeDTO> getContentBasedRecommendations(Map<String, Double> genrePreferences, int limit) {
        List<AnimeDTO> recommendations = new ArrayList<>();

        for (Map.Entry<String, Double> entry : genrePreferences.entrySet()) {
            String genre = entry.getKey();
            double weight = entry.getValue();
            int numToRecommend = (int) Math.ceil(limit * weight);

            List<Anime> animes = animeRepository.findTopRatedAnimesByGenre(
                    genre,
                    PageRequest.of(0, numToRecommend)
            );

            for (Anime anime : animes) {
                recommendations.add(new AnimeDTO(
                        anime.getId(),
                        anime.getTitulo(),
                        anime.getDescripcion(),
                        anime.getGenero(),
                        anime.getImagenUrl()
                ));
            }
        }

        return recommendations;
    }

    private List<AnimeDTO> getCollaborativeRecommendations(List<Usuario> similarUsers, int limit) {
        Map<Long, Integer> animeFrequency = new HashMap<>();

        for (Usuario user : similarUsers) {
            List<Favorito> favoritos = favoritoRepository.findByUsuario(user);
            for (Favorito favorito : favoritos) {
                animeFrequency.merge(favorito.getAnime().getId(), 1, Integer::sum);
            }
        }

        // Ordenar por frecuencia
        List<Long> recommendedAnimeIds = animeFrequency.entrySet().stream()
                .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(limit)
                .collect(Collectors.toList());

        return recommendedAnimeIds.stream()
                .map(animeRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(anime -> new AnimeDTO(
                        anime.getId(),
                        anime.getTitulo(),
                        anime.getDescripcion(),
                        anime.getGenero(),
                        anime.getImagenUrl()
                ))
                .collect(Collectors.toList());
    }

    private List<AnimeDTO> combineAndRankRecommendations(
            List<AnimeDTO> contentBased,
            List<AnimeDTO> collaborative,
            Map<String, Double> genrePreferences,
            int limit) {

        Map<Long, AnimeWithScore> animeScores = new HashMap<>();

        // Puntuar content-based
        for (int i = 0; i < contentBased.size(); i++) {
            AnimeDTO anime = contentBased.get(i);
            double score = (contentBased.size() - i) / (double) contentBased.size();
            score *= genrePreferences.getOrDefault(anime.getGenero(), 0.0);

            animeScores.put(anime.getId(), new AnimeWithScore(anime, score));
        }

        // Puntuar collaborative
        for (int i = 0; i < collaborative.size(); i++) {
            AnimeDTO anime = collaborative.get(i);
            double score = (collaborative.size() - i) / (double) collaborative.size();

            if (animeScores.containsKey(anime.getId())) {
                animeScores.get(anime.getId()).addScore(score * 0.5);
            } else {
                animeScores.put(anime.getId(), new AnimeWithScore(anime, score * 0.5));
            }
        }

        // Ordenar por score
        return animeScores.values().stream()
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .map(a -> a.anime)
                .limit(limit)
                .collect(Collectors.toList());
    }
// Agregar estos métodos a AdvancedRecommendationService.java
@Transactional(readOnly = true)
    public Map<String, Object> getDebugInfo(Long usuarioId) {
        Map<String, Object> debug = new HashMap<>();

        Usuario usuario = usuarioRepository.findById(usuarioId).orElseThrow();

        // Analizar preferencias
        Map<String, Double> genrePreferences = analyzeUserPreferences(usuario);

        // Obtener estadísticas del usuario
        List<Favorito> favoritos = favoritoRepository.findByUsuario(usuario);
        List<Votacion> votaciones = votacionRepository.findByUsuario(usuario);

        debug.put("usuarioId", usuarioId);
        debug.put("favoritosCount", favoritos.size());
        debug.put("votacionesCount", votaciones.size());
        debug.put("genrePreferences", genrePreferences);

        // Mostrar favoritos
        List<Map<String, Object>> favoritosInfo = favoritos.stream()
                .map(f -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("animeId", f.getAnime().getId());
                    info.put("titulo", f.getAnime().getTitulo());
                    info.put("genero", f.getAnime().getGenero());
                    return info;
                })
                .collect(Collectors.toList());
        debug.put("favoritos", favoritosInfo);

        // Mostrar votaciones altas
        List<Map<String, Object>> votacionesAltas = votaciones.stream()
                .filter(v -> v.getPuntuacion() >= 4.0)
                .map(v -> {
                    Map<String, Object> info = new HashMap<>();
                    info.put("animeId", v.getAnime().getId());
                    info.put("titulo", v.getAnime().getTitulo());
                    info.put("genero", v.getAnime().getGenero());
                    info.put("puntuacion", v.getPuntuacion());
                    return info;
                })
                .collect(Collectors.toList());
        debug.put("votacionesAltas", votacionesAltas);

        return debug;
    }
    private static class AnimeWithScore {
        AnimeDTO anime;
        double score;

        AnimeWithScore(AnimeDTO anime, double score) {
            this.anime = anime;
            this.score = score;
        }

        void addScore(double additional) {
            this.score += additional;
        }
    }
}