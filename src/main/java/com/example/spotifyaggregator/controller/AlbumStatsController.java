package com.example.spotifyaggregator.controller;

import com.example.spotifyaggregator.dto.AlbumStatsByArtistResponse;
import com.example.spotifyaggregator.dto.AlbumStatsByYearResponse;
import com.example.spotifyaggregator.dto.PagedResponse;
import com.example.spotifyaggregator.service.AlbumStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/albums")
@RequiredArgsConstructor
public class AlbumStatsController {

    private final AlbumStatsService albumStatsService;

    @GetMapping("/by-year")
    public Mono<ResponseEntity<PagedResponse<AlbumStatsByYearResponse>>> getStatsByYear(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return albumStatsService.getAlbumStatsByYear(page, size)
                .map(ResponseEntity::ok);
    }

    @GetMapping("/by-artist")
    public Mono<ResponseEntity<PagedResponse<AlbumStatsByArtistResponse>>> getStatsByArtist(
            @RequestParam String artist,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return albumStatsService.getAlbumStatsByArtist(artist, page, size)
                .map(ResponseEntity::ok);
    }
}
