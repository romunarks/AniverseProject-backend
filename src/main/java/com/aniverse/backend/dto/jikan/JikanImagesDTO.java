package com.aniverse.backend.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO para mapear el objeto images de la API Jikan
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class JikanImagesDTO {

    private JikanImageUrlsDTO jpg;

    private JikanImageUrlsDTO webp;

    public JikanImageUrlsDTO getJpg() {
        return jpg;
    }

    public void setJpg(JikanImageUrlsDTO jpg) {
        this.jpg = jpg;
    }

    public JikanImageUrlsDTO getWebp() {
        return webp;
    }

    public void setWebp(JikanImageUrlsDTO webp) {
        this.webp = webp;
    }
}