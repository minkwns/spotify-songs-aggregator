package com.example.spotifyaggregator.repository;

import com.example.spotifyaggregator.domain.SongLike;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface SongLikeRepository extends ReactiveCrudRepository<SongLike, Void> {

    Mono<Boolean> existsByUserIdAndSongId(Long userId, Long songId);
    Mono<Void> deleteByUserIdAndSongId(Long userId, Long songId);
}