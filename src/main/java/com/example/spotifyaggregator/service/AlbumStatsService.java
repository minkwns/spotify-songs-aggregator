package com.example.spotifyaggregator.service;

import com.example.spotifyaggregator.dto.AlbumStatsByArtistResponse;
import com.example.spotifyaggregator.dto.AlbumStatsByYearResponse;
import com.example.spotifyaggregator.dto.PagedResponse;
import com.example.spotifyaggregator.exception.AlbumStatsNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AlbumStatsService {
private final DatabaseClient databaseClient;

    public Mono<PagedResponse<AlbumStatsByYearResponse>> getAlbumStatsByYear(int page, int size) {
        String dataSql = """
            SELECT release_year,
                   COUNT(DISTINCT album) AS album_count
            FROM song
            GROUP BY release_year
            ORDER BY release_year
            LIMIT :limit OFFSET :offset
            """;

        String countSql = """
            SELECT COUNT(*) FROM (
                SELECT release_year
                FROM song 
                GROUP BY release_year
            ) AS years
            """;

        int offset = page * size;

        Mono<List<AlbumStatsByYearResponse>> contentMono = databaseClient.sql(dataSql)
                .bind("limit", size)
                .bind("offset", offset)
                .map((row, meta) -> new AlbumStatsByYearResponse(
                        row.get("release_year", Integer.class),
                        row.get("album_count", Long.class)
                ))
                .all()
                .collectList();

        Mono<Long> countMono = databaseClient.sql(countSql)
                .map((row, meta) -> row.get(0, Long.class))
                .one()
                .defaultIfEmpty(0L);

        return Mono.zip(contentMono, countMono)
                .map(tuple -> {
                    List<AlbumStatsByYearResponse> content = tuple.getT1();
                    long totalElements = tuple.getT2();
                    int totalPages = (int) Math.ceil((double) totalElements / size);
                    return new PagedResponse<>(content, page, size, totalElements, totalPages);
                });
    }

    public Mono<PagedResponse<AlbumStatsByArtistResponse>> getAlbumStatsByArtist(String artist, int page, int size) {
        String dataSql = """
            SELECT a.name AS artist_name,
                           YEAR(s.release_date) AS release_year,
                           COUNT(DISTINCT s.album) AS album_count
                    FROM song s
                    JOIN song_artist sa ON s.id = sa.song_id
                    JOIN artist a ON sa.artist_id = a.id
                    WHERE a.name = :artist
                    GROUP BY a.name, YEAR(s.release_date)
                    ORDER BY release_year
                    LIMIT :limit OFFSET :offset
            """;

        String countSql = """
            SELECT COUNT(*) AS total
            FROM (
                SELECT YEAR(s.release_date)
                FROM song s
                JOIN song_artist sa ON s.id = sa.song_id
                JOIN artist a ON sa.artist_id = a.id
                WHERE a.name = :artist
                GROUP BY a.name, YEAR(s.release_date)
            ) AS sub
            """;

        int offset = page * size;

        Mono<List<AlbumStatsByArtistResponse>> contentMono = databaseClient.sql(dataSql)
                .bind("artist", artist)
                .bind("limit", size)
                .bind("offset", offset)
                .map((row, meta) -> new AlbumStatsByArtistResponse(
                        row.get("artist_name", String.class),
                        row.get("release_year", Integer.class),
                        row.get("album_count", Long.class)
                ))
                .all()
                .collectList()
                .flatMap(list -> list.isEmpty()
                        ? Mono.error(new AlbumStatsNotFoundException())
                        : Mono.just(list)
                );;

        Mono<Long> countMono = databaseClient.sql(countSql)
                .bind("artist", artist)
                .map((row, meta) -> row.get("total", Long.class))
                .one()
                .defaultIfEmpty(0L);

        return Mono.zip(contentMono, countMono)
                .map(tuple -> {
                    List<AlbumStatsByArtistResponse> content = tuple.getT1();
                    long totalElements = tuple.getT2();
                    int totalPages = (int) Math.ceil((double) totalElements / size);
                    return new PagedResponse<>(content, page, size, totalElements, totalPages);
                });
    }
}