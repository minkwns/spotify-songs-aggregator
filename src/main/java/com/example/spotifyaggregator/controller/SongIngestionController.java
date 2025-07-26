package com.example.spotifyaggregator.controller;

import com.example.spotifyaggregator.dto.CommonResponse;
import com.example.spotifyaggregator.dto.SongIngestionResult;
import com.example.spotifyaggregator.service.SongIngestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/songs")
@RequiredArgsConstructor
public class SongIngestionController {

    private final SongIngestionService songIngestionService;

    @GetMapping("/ingest")
    public Mono<ResponseEntity<CommonResponse<SongIngestionResult>>> ingest() {
        return songIngestionService.ingest()
                .map(CommonResponse::success)
                .map(ResponseEntity::ok);
    }
}
