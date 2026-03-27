package com.aniverse.backend.service;

import com.aniverse.backend.exception.ResourceNotFoundException;
import com.aniverse.backend.model.Anime;
import com.aniverse.backend.repository.AnimeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ExternalAnimeService {

    private static final Logger log = LoggerFactory.getLogger(ExternalAnimeService.class);

    private final RestTemplate restTemplate;
    private final AnimeRepository animeRepository;

    @Value("${jikan.api.url:https://api.jikan.moe/v4}")
    private String JIKAN_API_URL;

    // Rate limiting: Jikan API tiene un límite de 3 peticiones por segundo y 60 por minuto
    private static final long RATE_LIMIT_DELAY = 334; // ms entre peticiones (aprox. 3 por segundo)
    private long lastRequestTime = 0;
    // Añadir constantes para tipos de listado
    public static final String CATEGORY_TRENDING = "trending";
    public static final String CATEGORY_RECENT = "recent";
    public static final String CATEGORY_TOP_RATED = "top_rated";
    public static final int DEFAULT_CATEGORY_LIMIT = 10; // Número base de animes por categoría

    // Cachés separados para cada categoría
    private final Map<String, List<Anime>> categoryCache = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> cacheTimes = new ConcurrentHashMap<>();

    public ExternalAnimeService(RestTemplate restTemplate, AnimeRepository animeRepository) {
        this.restTemplate = restTemplate;
        this.animeRepository = animeRepository;
    }

    /**
     * Obtiene animes en tendencia desde la API Jikan, los guarda en la base de datos,
     * y devuelve la lista de animes con IDs de base de datos válidos
     *
     * @return Lista de animes en tendencia guardados en la base de datos
     */
    //@Cacheable(value = "trendingAnimesCache", key = "'trending'", unless = "#result.isEmpty()")
    public List<Anime> fetchTrendingAnimes() {
        try {
            applyRateLimit(); // Mantenemos tu control de tasas si lo tienes implementado

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            log.info("Solicitando animes en tendencia desde la API Jikan");
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    JIKAN_API_URL + "/top/anime",
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getBody() == null || response.getBody().get("data") == null) {
                log.warn("Respuesta vacía o sin datos de Jikan API");
                return Collections.emptyList();
            }

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            log.info("Obtenidos {} animes desde la API Jikan", data.size());

            List<Anime> animesGuardados = new ArrayList<>();
            int procesados = 0, guardados = 0, actualizados = 0;

            for (Map<String, Object> item : data) {
                try {
                    procesados++;
                    // Usamos tu método existente para convertir los datos
                    Anime anime = convertToAnime(item);
                    if (anime == null) {
                        continue;
                    }

                    // Comprobar si el anime ya existe en nuestra base de datos por su jikanId
                    Optional<Anime> existingAnime = animeRepository.findByJikanId(anime.getJikanId());

                    if (existingAnime.isPresent()) {
                        // Si ya existe, considerar si necesitamos actualizar algún campo
                        Anime animeExistente = existingAnime.get();
                        boolean necesitaActualizacion = false;

                        // Verificar y actualizar campos si es necesario
                        if (anime.getTitulo() != null && !anime.getTitulo().equals(animeExistente.getTitulo())) {
                            animeExistente.setTitulo(anime.getTitulo());
                            necesitaActualizacion = true;
                        }

                        if (anime.getDescripcion() != null &&
                                (animeExistente.getDescripcion() == null ||
                                        !anime.getDescripcion().equals(animeExistente.getDescripcion()))) {
                            animeExistente.setDescripcion(anime.getDescripcion());
                            necesitaActualizacion = true;
                        }

                        if (anime.getImagenUrl() != null &&
                                (animeExistente.getImagenUrl() == null ||
                                        !anime.getImagenUrl().equals(animeExistente.getImagenUrl()))) {
                            animeExistente.setImagenUrl(anime.getImagenUrl());
                            necesitaActualizacion = true;
                        }

                        if (anime.getGenero() != null &&
                                (animeExistente.getGenero() == null ||
                                        !anime.getGenero().equals(animeExistente.getGenero()))) {
                            animeExistente.setGenero(anime.getGenero());
                            necesitaActualizacion = true;
                        }

                        // Solo guardar si hubo cambios
                        if (necesitaActualizacion) {
                            animeExistente = animeRepository.save(animeExistente);
                            log.info("Actualizado anime existente: {} (ID: {}, JikanID: {})",
                                    animeExistente.getTitulo(), animeExistente.getId(), animeExistente.getJikanId());
                            actualizados++;
                        }

                        animesGuardados.add(animeExistente);
                    } else {
                        // Si no existe, guardar como nuevo
                        Anime animeGuardado = animeRepository.save(anime);
                        log.info("Guardado nuevo anime: {} (ID: {}, JikanID: {})",
                                animeGuardado.getTitulo(), animeGuardado.getId(), animeGuardado.getJikanId());
                        animesGuardados.add(animeGuardado);
                        guardados++;
                    }

                } catch (Exception e) {
                    log.error("Error procesando anime individual: {}", e.getMessage(), e);
                }
            }

            log.info("Procesados {} animes: {} nuevos guardados, {} actualizados",
                    procesados, guardados, actualizados);
            return animesGuardados;
        } catch (Exception e) {
            log.error("Error obteniendo animes trending: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Busca animes basado en una consulta de texto
     *
     * @param query Término de búsqueda
     * @return Lista de animes que coinciden con la búsqueda
     */
    //@Cacheable(value = "searchAnimesCache", key = "#query", unless = "#result.isEmpty()")
    public List<Anime> searchAnimes(String query) {
      //  if (query == null || query.trim().isEmpty()) {
        //    return Collections.emptyList();
        //}

        try {
            applyRateLimit();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            String apiUrl;
            if (query != null && !query.trim().isEmpty()) {
                log.info("Searching animes with query: {}", query);
                String encodedQuery = org.springframework.web.util.UriUtils.encodeQueryParam(query, "UTF-8");
                apiUrl = JIKAN_API_URL + "/anime?q=" + encodedQuery + "&limit=10"; // Add a limit for broad searches
            } else {
                // For empty query, fetch top animes (similar to trending, but don't save here)
                // This is what the controller's animesFromExternalSearch = externalAnimeService.searchAnimes(""); call will now use.
                log.info("Fetching broad list of animes (top animes for empty query)");
                apiUrl = JIKAN_API_URL + "/top/anime?limit=10"; // Fetch, e.g., top 10 as a broad sample
                // You could also use other Jikan endpoints like "/seasons/now" or "/watch/promos"
            }

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getBody() == null || response.getBody().get("data") == null) {
                log.warn("Respuesta vacía o sin datos de Jikan API para la consulta/búsqueda amplia.");
                return Collections.emptyList();
            }

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            log.info("Obtenidos {} animes desde Jikan API para consulta '{}'/búsqueda amplia", data.size(), query);

            List<Anime> animes = new ArrayList<>();
            for (Map<String, Object> item : data) {
                Anime anime = convertToAnime(item); // convertToAnime creates Anime with id=null
                if (anime != null) {
                    // CRITICAL: DO NOT SAVE ANIME HERE if you want them to be "purely external"
                    animes.add(anime);
                }
            }
            log.info("Returning {} animes from searchAnimes (query: '{}'). These animes have id=null unless already in DB from other operations.", animes.size(), query);
            return animes;
        } catch (Exception e) {
            log.error("Error searching animes with query '{}': {}", query, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Busca un anime por su ID de Jikan (MAL ID) y lo guarda en la base de datos si no existe
     *
     * @param jikanId ID de Jikan (MAL ID)
     * @return El anime encontrado o guardado
     * @throws ResourceNotFoundException si el anime no se puede encontrar en Jikan
     */
    @Transactional
    public Anime findOrSaveExternalAnime(Long jikanId) {
        if (jikanId == null) {
            throw new IllegalArgumentException("El jikanId no puede ser nulo");
        }

        // Buscar si ya existe en DB (no eliminado)
        Optional<Anime> existingAnime = animeRepository.findByJikanIdAndEliminadoFalse(jikanId);
        if (existingAnime.isPresent()) {
            log.debug("Anime con jikanId {} encontrado localmente", jikanId);
            return existingAnime.get();
        }

        // Buscar si existe pero está eliminado
        existingAnime = animeRepository.findByJikanId(jikanId);
        if (existingAnime.isPresent() && existingAnime.get().isEliminado()) {
            log.info("Restaurando anime con jikanId {} que estaba eliminado", jikanId);
            Anime anime = existingAnime.get();
            anime.setEliminado(false);
            anime.setFechaEliminacion(null);
            return animeRepository.save(anime);
        }

        // Si no existe, obtenerlo de API y guardarlo
        try {
            applyRateLimit();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    JIKAN_API_URL + "/anime/" + jikanId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getBody() == null || response.getBody().get("data") == null) {
                throw new ResourceNotFoundException("Anime no encontrado en Jikan con ID: " + jikanId);
            }

            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            Anime anime = convertToAnime(data);

            if (anime != null) {
                log.info("Guardando nuevo anime con jikanId {}: {}", jikanId, anime.getTitulo());
                // Verificación adicional de seguridad para la descripción antes de guardar
                if (anime.getDescripcion() != null && anime.getDescripcion().length() > 240) {
                    log.warn("Truncando descripción en findOrSaveExternalAnime para animeId: {}", anime.getJikanId());
                    anime.setDescripcion(anime.getDescripcion().substring(0, 237) + "...");
                }

// Verificación para otros campos de texto
                if (anime.getGenero() != null && anime.getGenero().length() > 200) {
                    anime.setGenero(anime.getGenero().substring(0, 197) + "...");
                }

                if (anime.getTitulo() != null && anime.getTitulo().length() > 100) {
                    anime.setTitulo(anime.getTitulo().substring(0, 97) + "...");
                }

                return animeRepository.save(anime);
            }

            throw new ResourceNotFoundException("Error al procesar datos de anime con ID: " + jikanId);
        } catch (HttpClientErrorException.NotFound e) {
            log.error("Anime no encontrado en Jikan API: {}", jikanId);
            throw new ResourceNotFoundException("Anime no encontrado en Jikan con ID: " + jikanId);
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error al obtener anime de Jikan API: {}", e.getMessage(), e);
            throw new RuntimeException("Error al comunicarse con Jikan API: " + e.getMessage(), e);
        }
    }

    /**
     * Convierte datos de la API Jikan a una entidad Anime
     *
     * @param data Datos del anime desde Jikan
     * @return Entidad Anime mapeada
     */
    // En ExternalAnimeService.java

    private Anime convertToAnime(Map<String, Object> data) {
        try {
            Anime anime = new Anime();
            anime.setId(null); // ID será asignado por la BD

            // 1️⃣ JIKAN ID (OBLIGATORIO)
            Number malIdNumber = (Number) data.get("mal_id");
            if (malIdNumber == null) {
                log.error("Dato 'mal_id' es nulo o no es un número en la respuesta de Jikan.");
                return null;
            }
            long jikanId = malIdNumber.longValue();
            anime.setJikanId(jikanId);

            // 2️⃣ TÍTULO (OBLIGATORIO CON FALLBACK)
            String titulo = (String) data.get("title");
            if (titulo == null || titulo.trim().isEmpty()) {
                titulo = "Anime " + jikanId; // Valor por defecto
                log.warn("Anime sin título desde Jikan, usando fallback: {}", titulo);
            }
            // Truncar título si es muy largo
            final int MAX_TITULO_LENGTH = 100;
            if (titulo.length() > MAX_TITULO_LENGTH) {
                titulo = titulo.substring(0, MAX_TITULO_LENGTH - 3) + "...";
                log.debug("Truncando título para jikanId {}: longitud original {}", jikanId, titulo.length() + 3);
            }
            anime.setTitulo(titulo);

            // 3️⃣ DESCRIPCIÓN (CON FALLBACK)
            Object synopsisObj = data.get("synopsis");
            String descripcion = "Sin descripción disponible"; // Valor por defecto

            if (synopsisObj != null) {
                String synopsisStr = synopsisObj.toString().trim();
                if (!synopsisStr.isEmpty() && !synopsisStr.equalsIgnoreCase("null")) {
                    descripcion = synopsisStr;
                }
            }

            // Truncar descripción
            final int MAX_DESCRIPCION_LENGTH = 240;
            if (descripcion.length() > MAX_DESCRIPCION_LENGTH) {
                log.debug("Truncando descripción para jikanId {}: de {} a {} caracteres",
                        jikanId, descripcion.length(), MAX_DESCRIPCION_LENGTH);
                descripcion = descripcion.substring(0, MAX_DESCRIPCION_LENGTH - 3) + "...";
            }
            anime.setDescripcion(descripcion);

            // 4️⃣ IMAGEN URL (OPCIONAL)
            String imagenUrl = null;
            Map<String, Object> images = (Map<String, Object>) data.get("images");
            if (images != null) {
                Map<String, Object> jpg = (Map<String, Object>) images.get("jpg");
                if (jpg != null) {
                    imagenUrl = (String) jpg.get("large_image_url");
                    if (imagenUrl == null || imagenUrl.trim().isEmpty()) {
                        imagenUrl = (String) jpg.get("image_url"); // Fallback a imagen normal
                    }
                }
            }
            anime.setImagenUrl(imagenUrl);

            // 5️⃣ GÉNEROS (CON FALLBACK)
            String genero = "Sin clasificar"; // Valor por defecto
            List<Map<String, Object>> genres = (List<Map<String, Object>>) data.get("genres");
            if (genres != null && !genres.isEmpty()) {
                String generosConcatenados = genres.stream()
                        .map(genreMap -> (String) genreMap.get("name"))
                        .filter(Objects::nonNull)
                        .filter(name -> !name.trim().isEmpty())
                        .collect(Collectors.joining(", "));

                if (!generosConcatenados.isEmpty()) {
                    genero = generosConcatenados;
                }
            }

            // Truncar géneros
            final int MAX_GENERO_LENGTH = 200;
            if (genero.length() > MAX_GENERO_LENGTH) {
                genero = genero.substring(0, MAX_GENERO_LENGTH - 3) + "...";
                log.debug("Truncando géneros para jikanId {}: longitud original {}", jikanId, genero.length() + 3);
            }
            anime.setGenero(genero);

            // 6️⃣ TEMPORADA (CON FALLBACK) - ✅ SIEMPRE ASIGNA VALOR
            String temporada = "Desconocida"; // Valor por defecto
            String season = (String) data.get("season");
            if (season != null && !season.trim().isEmpty() && !season.equalsIgnoreCase("null")) {
                try {
                    // Capitalizar primera letra
                    String temporadaFormateada = season.trim().substring(0, 1).toUpperCase() +
                            season.trim().substring(1).toLowerCase();

                    // Truncar si es necesario
                    final int MAX_TEMPORADA_LENGTH = 20;
                    if (temporadaFormateada.length() > MAX_TEMPORADA_LENGTH) {
                        temporadaFormateada = temporadaFormateada.substring(0, MAX_TEMPORADA_LENGTH);
                    }
                    temporada = temporadaFormateada;
                } catch (Exception e) {
                    log.warn("Error procesando temporada '{}' para jikanId {}: {}",
                            season, jikanId, e.getMessage());
                    // Mantiene el valor por defecto "Desconocida"
                }
            }
            anime.setTemporada(temporada); // ✅ SIEMPRE asigna un valor

            // 7️⃣ AÑO (CON FALLBACK) - ✅ SIEMPRE ASIGNA VALOR
            Integer year = null;
            Object yearObj = data.get("year");

            if (yearObj != null) {
                try {
                    if (yearObj instanceof Number) {
                        int yearValue = ((Number) yearObj).intValue();
                        // Validar año razonable (1900-2030)
                        if (yearValue >= 1900 && yearValue <= 2030) {
                            year = yearValue;
                        } else {
                            log.warn("Año fuera de rango para jikanId {}: {}, usando 1900", jikanId, yearValue);
                        }
                    } else if (yearObj instanceof String) {
                        String yearStr = ((String) yearObj).trim();
                        if (!yearStr.isEmpty() && !yearStr.equalsIgnoreCase("null")) {
                            int yearValue = Integer.parseInt(yearStr);
                            if (yearValue >= 1900 && yearValue <= 2030) {
                                year = yearValue;
                            } else {
                                log.warn("Año fuera de rango para jikanId {}: {}, usando 1900", jikanId, yearValue);
                            }
                        }
                    }
                } catch (NumberFormatException e) {
                    log.warn("Error parseando año '{}' para jikanId {}: {}", yearObj, jikanId, e.getMessage());
                } catch (Exception e) {
                    log.warn("Error procesando año para jikanId {}: {}", jikanId, e.getMessage());
                }
            }

            // ✅ SIEMPRE asignar un valor al año
            if (year != null) {
                anime.setAnyo(year);
            } else {
                anime.setAnyo(1900); // Valor por defecto
                log.debug("Anime sin año válido desde Jikan, usando 1900 por defecto: {} (JikanID: {})",
                        anime.getTitulo(), jikanId);
            }

            // 8️⃣ ESTADO DE ELIMINACIÓN
            anime.setEliminado(false);

            log.debug("✅ Anime convertido exitosamente: JikanID={}, Título='{}', Año={}, Temporada='{}'",
                    jikanId, anime.getTitulo(), anime.getAnyo(), anime.getTemporada());

            return anime;

        } catch (Exception e) {
            log.error("❌ Error crítico convirtiendo datos de Jikan a Anime. Datos: {}. Error: {}",
                    data, e.getMessage(), e);
            return null;
        }
    }
    /**
     * Implementa un simple rate limiting para respetar los límites de la API Jikan
     */
    private synchronized void applyRateLimit() {
        long now = System.currentTimeMillis();
        long elapsedTime = now - lastRequestTime;

        if (elapsedTime < RATE_LIMIT_DELAY) {
            try {
                Thread.sleep(RATE_LIMIT_DELAY - elapsedTime);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        lastRequestTime = System.currentTimeMillis();
    }

    // En ExternalAnimeService.java
    /**
     * Busca un anime por su ID de Jikan (MAL ID) pero no lo guarda en la base de datos
     */
    public Anime findExternalAnime(Long jikanId) {
        if (jikanId == null) {
            throw new IllegalArgumentException("El jikanId no puede ser nulo");
        }

        // Primero verificar si ya existe en la BD
        Optional<Anime> existingAnime = animeRepository.findByJikanId(jikanId);
        if (existingAnime.isPresent()) {
            return existingAnime.get();
        }

        // Si no existe, obtenerlo de la API pero NO guardarlo
        try {
            applyRateLimit();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    JIKAN_API_URL + "/anime/" + jikanId,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getBody() == null || response.getBody().get("data") == null) {
                throw new ResourceNotFoundException("Anime no encontrado en Jikan con ID: " + jikanId);
            }

            Map<String, Object> data = (Map<String, Object>) response.getBody().get("data");
            Anime anime = convertToAnime(data);

            if (anime == null) {
                throw new ResourceNotFoundException("Error al procesar datos de anime con ID: " + jikanId);
            }

            // Importante: NO guarda en BD
            return anime;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ResourceNotFoundException("Anime no encontrado en Jikan con ID: " + jikanId);
        } catch (Exception e) {
            log.error("Error al obtener anime de Jikan API: {}", e.getMessage(), e);
            throw new RuntimeException("Error al comunicarse con Jikan API: " + e.getMessage(), e);
        }
    }
    /**
     * Obtiene animes de una categoría específica de Jikan
     * @param category Categoría a obtener (trending, recent, top_rated)
     * @param limit Número máximo de animes a retornar
     * @return Lista de animes de la categoría solicitada
     */
    /**
     * Obtiene animes de una categoría específica de Jikan
     * @param category Categoría a obtener (trending, recent, top_rated)
     * @param limit Número máximo de animes a retornar
     * @return Lista de animes de la categoría solicitada
     */
    public List<Anime> getAnimesByCategory(String category, int limit) {
        // Validar el límite para evitar excesos
        int safeLimit = Math.min(limit, 25); // Máximo 25 animes por categoría

        // Verificar si existe un caché válido para esta categoría
        String cacheKey = category + "_" + safeLimit;
        if (categoryCache.containsKey(cacheKey)) {
            LocalDateTime cacheTime = cacheTimes.getOrDefault(cacheKey, LocalDateTime.MIN);
            long cacheMinutes = CATEGORY_TRENDING.equals(category) ? 30 : 60;
            if (ChronoUnit.MINUTES.between(cacheTime, LocalDateTime.now()) < cacheMinutes) {
                log.info("Usando caché para '{}' animes ({}). Edad: {} minutos",
                        category, safeLimit, ChronoUnit.MINUTES.between(cacheTime, LocalDateTime.now()));
                // Devolver una copia para evitar modificaciones externas al caché
                return new ArrayList<>(categoryCache.get(cacheKey));
            }
        }

        try {
            applyRateLimit();

            String apiUrl;
            switch (category) {
                case CATEGORY_TRENDING:
                    apiUrl = JIKAN_API_URL + "/top/anime?limit=" + safeLimit;
                    break;
                case CATEGORY_RECENT:
                    apiUrl = JIKAN_API_URL + "/seasons/now?limit=" + safeLimit;
                    break;
                case CATEGORY_TOP_RATED:
                    apiUrl = JIKAN_API_URL + "/top/anime?filter=bypopularity&limit=" + safeLimit;
                    break;
                default:
                    log.warn("Categoría desconocida '{}', usando /top/anime por defecto", category);
                    apiUrl = JIKAN_API_URL + "/top/anime?limit=" + safeLimit;
            }

            log.info("Solicitando {} animes (límite {}) a Jikan API: {}", category, safeLimit, apiUrl);

            HttpHeaders headers = new HttpHeaders();
            headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.GET,
                    entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {}
            );

            if (response.getBody() == null || response.getBody().get("data") == null) {
                log.warn("Respuesta vacía o sin datos de Jikan API para categoría '{}'", category);
                return fallbackToCache(cacheKey);
            }

            List<Map<String, Object>> data = (List<Map<String, Object>>) response.getBody().get("data");
            log.info("Obtenidos {} animes desde la API Jikan para categoría '{}'", data.size(), category);

            List<Anime> animesResultList = new ArrayList<>();

            for (Map<String, Object> item : data) {
                Anime animeFromJikan = convertToAnime(item); // Convierte el item de Jikan
                if (animeFromJikan != null) {
                    // SIMPLEMENTE AÑADIR EL ANIME DE JIKAN DIRECTAMENTE
                    // Este anime tendrá id=null (a menos que convertToAnime lo cambie)
                    // y no tendrá votaciones de tu BD.
                    animesResultList.add(animeFromJikan);
                    log.debug("Añadiendo anime directamente de Jikan para categoría '{}': JikanID={}, Título={}",
                            category, animeFromJikan.getJikanId(), animeFromJikan.getTitulo());
                }
            }

            log.info("Devueltos {} animes directamente de Jikan para categoría '{}'", animesResultList.size(), category);

            // Guardar en caché la lista resultante
            categoryCache.put(cacheKey, Collections.unmodifiableList(new ArrayList<>(animesResultList)));
            cacheTimes.put(cacheKey, LocalDateTime.now());

            return animesResultList;
        } catch (Exception e) {
            log.error("Error crítico obteniendo animes de categoría '{}': {}", category, e.getMessage(), e);
            return fallbackToCache(cacheKey); // Devolver caché antiguo si la nueva carga falla
        }


    }




    // Método auxiliar para devolver caché anterior en caso de error
    private List<Anime> fallbackToCache(String cacheKey) {
        if (categoryCache.containsKey(cacheKey)) {
            log.warn("Devolviendo caché vencido debido a error en API");
            return new ArrayList<>(categoryCache.get(cacheKey));
        }
        return Collections.emptyList();
    }


    @Transactional
    public Anime findOrCreateAnimeByJikanId(Long jikanId, String titulo, String imagenUrl,
                                            String tipo, Integer episodios, String estado,
                                            Double puntuacion, String generos, String sinopsis) {
        try {
            // Primero intentar encontrar el anime existente
            Optional<Anime> animeExistente = animeRepository.findByJikanId(jikanId);

            if (animeExistente.isPresent()) {
                // Si existe, actualizar datos si están disponibles
                Anime anime = animeExistente.get();
                boolean actualizado = false;

                if (titulo != null && !titulo.isEmpty() && !titulo.equals(anime.getTitulo())) {
                    anime.setTitulo(titulo);
                    actualizado = true;
                }

                if (imagenUrl != null && !imagenUrl.isEmpty() && !imagenUrl.equals(anime.getImagenUrl())) {
                    anime.setImagenUrl(imagenUrl);
                    actualizado = true;
                }

                // Guardar solo si hubo cambios
                if (actualizado) {
                    return animeRepository.save(anime);
                }

                return anime;
            } else {
                // Si no existe, crear nuevo anime
                Anime nuevoAnime = new Anime();
                nuevoAnime.setJikanId(jikanId);
                nuevoAnime.setTitulo(titulo != null && !titulo.isEmpty() ? titulo : "Anime " + jikanId);
                nuevoAnime.setImagenUrl(imagenUrl);
                nuevoAnime.setEliminado(false);

                return animeRepository.save(nuevoAnime);
            }

        } catch (Exception e) {
            System.err.println("Error creando/encontrando anime: " + e.getMessage());
            e.printStackTrace();

            // Como fallback, crear un anime básico
            Anime animeBasico = new Anime();
            animeBasico.setJikanId(jikanId);
            animeBasico.setTitulo(titulo != null && !titulo.isEmpty() ? titulo : "Anime " + jikanId);
            animeBasico.setImagenUrl(imagenUrl);
            animeBasico.setEliminado(false);

            return animeRepository.save(animeBasico);
        }
    }
}