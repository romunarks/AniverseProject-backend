package com.aniverse.backend.dto.jikan;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JikanAnimeDataDTO {

    @JsonProperty("mal_id")
    private Long malId;

    private String title;

    private String synopsis;

    private JikanImagesDTO images;

    private List<JikanGenreDTO> genres;

    @JsonProperty("year")
    private Integer year;

    private String season;

    // Constructor vacío
    public JikanAnimeDataDTO() {
    }

    // Getters y setters
    public Long getMalId() {
        return malId;
    }

    public void setMalId(Long malId) {
        this.malId = malId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public void setSynopsis(String synopsis) {
        this.synopsis = synopsis;
    }

    public JikanImagesDTO getImages() {
        return images;
    }

    public void setImages(JikanImagesDTO images) {
        this.images = images;
    }

    public List<JikanGenreDTO> getGenres() {
        return genres;
    }

    public void setGenres(List<JikanGenreDTO> genres) {
        this.genres = genres;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getSeason() {
        return season;
    }

    public void setSeason(String season) {
        this.season = season;
    }
}