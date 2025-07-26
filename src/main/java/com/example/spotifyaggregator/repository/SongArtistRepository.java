package com.example.spotifyaggregator.repository;

import com.example.spotifyaggregator.domain.SongArtist;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SongArtistRepository extends ReactiveCrudRepository<SongArtist, Void> {
}
