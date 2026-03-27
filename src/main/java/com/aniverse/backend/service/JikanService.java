package com.aniverse.backend.service;

import com.aniverse.backend.dto.jikan.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Servicio para comunicarse con la API de Jikan (MyAnimeList)
 */
@Service
public class JikanService {

    private static final Logger log = LoggerFactory.getLogger(JikanService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${jikan.api.url:https://api.jikan.moe/v4}")
    private String JIKAN_API_URL;

    // Rate limiting
    private static final long RATE_LIMIT_DELAY = 1000; // 1 segundo entre peticiones
    private long lastRequestTime = 0;

    public JikanService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * Obtiene un anime por su ID de MyAnimeList
     */
    public Optional<JikanAnimeDataDTO> getAnimeById(Long malId) {
        if (malId == null) {
            return Optional.empty();
        }

        applyRateLimit();

        try {
            String url = JIKAN_API_URL + "/anime/" + malId;
            log.debug("Consultando Jikan API: {}", url);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> responseBody = response.getBody();
                Object data = responseBody.get("data");

                if (data != null) {
                    // Convertir el objeto Map a JikanAnimeDataDTO usando ObjectMapper
                    JikanAnimeDataDTO animeData = objectMapper.convertValue(data, JikanAnimeDataDTO.class);
                    return Optional.of(animeData);
                }
            }

            return Optional.empty();
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Anime con ID {} no encontrado en Jikan", malId);
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error al obtener anime de Jikan: {}", e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Busca animes por título
     */
    public List<JikanAnimeDataDTO> searchAnimes(String query) {
        if (query == null || query.trim().isEmpty()) {
            return Collections.emptyList();
        }

        applyRateLimit();

        try {
            String encodedQuery = org.springframework.web.util.UriUtils.encodeQueryParam(query, "UTF-8");
            String url = JIKAN_API_URL + "/anime?q=" + encodedQuery;

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.getBody().get("data");

                if (dataList != null && !dataList.isEmpty()) {
                    List<JikanAnimeDataDTO> results = new ArrayList<>();

                    for (Map<String, Object> item : dataList) {
                        JikanAnimeDataDTO animeData = objectMapper.convertValue(item, JikanAnimeDataDTO.class);
                        results.add(animeData);
                    }

                    return results;
                }
            }

            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error al buscar animes en Jikan: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Obtiene los animes en tendencia
     */
    public List<JikanAnimeDataDTO> getTrendingAnimes() {
        applyRateLimit();

        try {
            String url = JIKAN_API_URL + "/top/anime";

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<Map<String, Object>> dataList = (List<Map<String, Object>>) response.getBody().get("data");

                if (dataList != null && !dataList.isEmpty()) {
                    List<JikanAnimeDataDTO> results = new ArrayList<>();

                    for (Map<String, Object> item : dataList) {
                        JikanAnimeDataDTO animeData = objectMapper.convertValue(item, JikanAnimeDataDTO.class);
                        results.add(animeData);
                    }

                    return results;
                }
            }

            return Collections.emptyList();
        } catch (Exception e) {
            log.error("Error al obtener animes en tendencia de Jikan: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Implementa rate limiting para evitar ser bloqueado por la API
     */
    private synchronized void applyRateLimit() {
        long now = System.currentTimeMillis();
        long elapsed = now - lastRequestTime;

        if (elapsed < RATE_LIMIT_DELAY) {
            try {
                Thread.sleep(RATE_LIMIT_DELAY - elapsed);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastRequestTime = System.currentTimeMillis();
    }
}