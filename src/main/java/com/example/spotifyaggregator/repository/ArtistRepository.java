package com.example.spotifyaggregator.repository;

import com.example.spotifyaggregator.domain.Artist;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ArtistRepository extends ReactiveCrudRepository<Artist, Long> {
    Mono<Artist> findByName(String name);
}
