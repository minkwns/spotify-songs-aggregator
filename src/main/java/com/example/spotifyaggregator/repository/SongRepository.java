package com.example.spotifyaggregator.repository;

import com.example.spotifyaggregator.domain.Song;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface SongRepository extends ReactiveCrudRepository<Song, Long> {
    Mono<Song> findByIsrcAndTitle(String isrc, String title);
}
