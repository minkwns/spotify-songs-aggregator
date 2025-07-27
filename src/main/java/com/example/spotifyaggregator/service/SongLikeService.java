package com.example.spotifyaggregator.service;

import com.example.spotifyaggregator.repository.SongLikeRepository;
import com.example.spotifyaggregator.dto.SongLikeAck;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongLikeService {

    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final SongLikeRepository songLikeRepository;
    private final DatabaseClient databaseClient;
    private static final String SONG_LIKE_KEY_PREFIX = "song:likes:";

    public Mono<SongLikeAck> likeSong(Long songId, Long userId) {
        String minuteKey = getCurrentMinuteKey();
        long timestamp = System.currentTimeMillis();
        String member = "like:" + songId + ":" + userId + ":" + timestamp;

        return songLikeRepository.existsByUserIdAndSongId(userId, songId)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.just(new SongLikeAck(songId, "이미 좋아요를 누르셨습니다."));
                    }

                    return databaseClient.sql("INSERT INTO song_like (user_id, song_id, liked_at) VALUES (:userId, :songId, :likedAt)")
                            .bind("userId", userId)
                            .bind("songId", songId)
                            .bind("likedAt", LocalDateTime.now())
                            .then()
                            .then(reactiveRedisTemplate.opsForZSet().add(minuteKey, member, timestamp))
                            .then(reactiveRedisTemplate.expire(minuteKey, Duration.ofHours(2)))
                            .thenReturn(new SongLikeAck(songId, "좋아요가 반영되었습니다."));
                });
    }

    public Mono<SongLikeAck> unlikeSong(Long songId, Long userId) {
        String minuteKey = getCurrentMinuteKey();
        long timestamp = System.currentTimeMillis();
        String member = "unlike:" + songId + ":" + userId + ":" + timestamp;

        return songLikeRepository.existsByUserIdAndSongId(userId, songId)
                .flatMap(exists -> {
                    if (!exists) {
                        return Mono.just(new SongLikeAck(songId, "좋아요를 누르지 않으셨습니다."));
                    }

                    return songLikeRepository.deleteByUserIdAndSongId(userId, songId)
                            .then(reactiveRedisTemplate.opsForZSet().add(minuteKey, member, timestamp))
                            .then(reactiveRedisTemplate.expire(minuteKey, Duration.ofHours(2)))
                            .thenReturn(new SongLikeAck(songId, "좋아요가 취소되었습니다."));
                });
    }

    private String getCurrentMinuteKey() {
        return SONG_LIKE_KEY_PREFIX + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }
}

