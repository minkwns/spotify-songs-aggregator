package com.example.spotifyaggregator.service;

import com.example.spotifyaggregator.dto.SongIngestionResult;
import com.example.spotifyaggregator.dto.SongWithArtists;
import com.example.spotifyaggregator.exception.ErrorCode;
import com.example.spotifyaggregator.exception.SongIngestionException;
import com.example.spotifyaggregator.repository.ArtistRepository;
import com.example.spotifyaggregator.repository.SongArtistRepository;
import com.example.spotifyaggregator.repository.SongRepository;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.spotifyaggregator.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import static com.example.spotifyaggregator.util.DateUtil.parseReleaseDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongIngestionService {

    private final SongRepository songRepository;
    private final ArtistRepository artistRepository;
    private final SongArtistRepository songArtistRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final int PARALLELISM = 10;

    @Value("${ingestion.file.path}")
    private String filePath;

    public Mono<SongIngestionResult> ingest() {
        return ingestFromJsonFile(filePath);
    }

    /**
     * 1) NDJSON 스트리밍 파싱
     * 2) 한 건씩 순차 처리(concurrency 조정)
     * 3) 중복 ISRC는 skip
     * 4) 기타 에러는 DLQ 에 모아서 최대 2회 재시도 후 skip
     */
    public Mono<SongIngestionResult> ingestFromJsonFile(String classpathResource) {
        AtomicInteger successCount = new AtomicInteger(0);
        Queue<Song> failedSongs = new ConcurrentLinkedQueue<>(); // DLQ
        objectMapper.enable(JsonReadFeature.ALLOW_NON_NUMERIC_NUMBERS.mappedFeature());
        objectMapper.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION.mappedFeature());

        long startTime = System.currentTimeMillis();
        log.info("Spotify Songs 메타 데이터에 대해 저장 배치 시작합니다. Raw data classpath : {}", classpathResource);
        return Flux.using(
                        () -> new BufferedReader(new InputStreamReader(
                                new ClassPathResource(classpathResource).getInputStream())),
                        reader -> Flux.fromStream(reader.lines()),
                        reader -> {
                            try {
                                reader.close();
                            } catch (IOException e) {
                                throw new SongIngestionException(ErrorCode.SONG_INGESTION_ERROR);
                            }
                        }
                )
                .flatMap(line ->
                        Mono.fromCallable(() -> objectMapper.readTree(line))
                                .map(this::mapJsonToSongWithArtists)
                                .onErrorResume(e -> {
                                    log.error("파싱 에러 라인 : {}, error: {}", line, e.getMessage());
                                    return Mono.empty();
                                })
                )
                .flatMapSequential(swa ->
                        saveSongAndArtists(swa, failedSongs)
                                .doOnSuccess(v -> successCount.incrementAndGet()),
                        PARALLELISM
                )
                .collectList()
                .map(savedList -> {
                    int success = successCount.get();
                    int failure = failedSongs.size();
                    log.info("데이터 수집이 완료되었습니다 : 성공 row(s) = {}, 실패 row(s) = {}, 소요 시간 = {}ms, " +
                                    "실패 건에 대해 등록 재시도를 시작하겠습니다.",
                            success, failure, System.currentTimeMillis() - startTime);

                    retryFromDlq(failedSongs); // DLQ 실행 비동기로 진행
                    return new SongIngestionResult(success, failure);
                });
    }

    private Mono<Void> saveSongAndArtists(SongWithArtists swa, Queue<Song> failedSongs) {
        return songRepository.save(swa.song())
                .onErrorResume(DuplicateKeyException.class, ex -> {
                    log.debug("노래 중복 발생 Skip 처리 : {} - {}", swa.song().getIsrc(), swa.song().getTitle());
                    return songRepository.findByIsrcAndTitle(swa.song().getIsrc(), swa.song().getTitle());
                })
                .flatMap(savedSong ->
                        Flux.fromIterable(swa.artists())
                                .flatMap(name ->
                                        artistRepository.save(Artist.builder().name(name).build())
                                                .onErrorResume(DuplicateKeyException.class, ex -> {
                                                    log.debug("아티스트 중복 Skip 처리 : {}", name);
                                                    return artistRepository.findByName(name);
                                                })
                                )
                                .flatMap(artist ->
                                        songArtistRepository.save(SongArtist.builder()
                                                        .songId(savedSong.getId())
                                                        .artistId(artist.getId())
                                                        .build())
                                                .onErrorResume(DuplicateKeyException.class, ex -> {
                                                    log.debug("최종 등록 리소스 중복 Skip 처리 : song={}, artist={}",
                                                            savedSong.getId(), artist.getId());
                                                    return Mono.empty();
                                                })
                                )
                                .then()
                )
                .onErrorResume(ex -> {
                    log.warn("중복 실패 : {}, error Msg : {}", swa.song().getIsrc(), ex.toString());
                    failedSongs.add(swa.song());
                    return Mono.empty();
                });
    }

    private SongWithArtists mapJsonToSongWithArtists(JsonNode node) {
        Song song = Song.builder()
                .isrc(node.path("ISRC").asText())
                .title(node.path("song").asText())
                .album(node.path("Album").asText())
                .releaseDate(parseReleaseDate(node.path("Release Date").asText()))
                .releaseYear(parseReleaseDate(node.path("Release Date").asText()).getYear())
                .genre(node.path("Genre").asText())
                .explicit("Yes".equalsIgnoreCase(node.path("Explicit").asText()))
                .popularity(node.path("Popularity").asInt())
                .build();

        List<String> artistNames = Arrays.stream(node.path("Artist(s)").asText().split("[;,]"))
                .map(String::trim)
                .filter(name -> !name.isEmpty())
                .distinct()
                .toList();

        return new SongWithArtists(song, artistNames);
    }

    private void retryFromDlq(Queue<Song> dlq) {
        Flux.fromIterable(dlq)
                .flatMap(song ->
                        songRepository.save(song)
                                .retryWhen(Retry.backoff(2, Duration.ofSeconds(1)))
                                .onErrorResume(ex -> {
                                    log.error("DLQ 재등록에 실패 하였습니다 : song_id : {}, song_isrc : {}, msg : {}", song.getId(), song.getIsrc(), ex.getMessage());
                                    return Mono.empty();
                                })
                )
                .doOnComplete(() -> log.info("DLQ 실패 작업에 대해 재등록 배치가 완료되었습니다."))
                .subscribe();
    }
}
