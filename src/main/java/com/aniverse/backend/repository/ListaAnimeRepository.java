package com.aniverse.backend.repository;

import com.aniverse.backend.model.Anime;
import com.aniverse.backend.model.Lista;
import com.aniverse.backend.model.ListaAnime;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ListaAnimeRepository extends JpaRepository<ListaAnime, Long> {
    List<ListaAnime> findByLista(Lista lista);

    Optional<ListaAnime> findByListaAndAnime(Lista lista, Anime anime);

    boolean existsByListaAndAnime(Lista lista, Anime anime);
}