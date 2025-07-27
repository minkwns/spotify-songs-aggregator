package com.example.spotifyaggregator.controller;

import com.example.spotifyaggregator.dto.CommonResponse;
import com.example.spotifyaggregator.dto.SongLikeAck;
import com.example.spotifyaggregator.service.SongLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;


@RestController
@RequiredArgsConstructor
@RequestMapping("/api/songs")
public class SongLikeController {

    private final SongLikeService songLikeService;

    @PostMapping("{songId}/like")
    public Mono<ResponseEntity<CommonResponse<SongLikeAck>>> likeSong(
            @PathVariable Long songId,
            @RequestHeader("User-Id") Long userId
    ) {
        return songLikeService.likeSong(songId, userId)
                .map(CommonResponse::success)
                .map(ResponseEntity::ok);
    }

    @DeleteMapping("{songId}/unlike")
    public Mono<ResponseEntity<CommonResponse<SongLikeAck>>> unlikeSong(
            @PathVariable Long songId,
            @RequestHeader("User-Id") Long userId
    ) {
        return songLikeService.unlikeSong(songId, userId)
                .map(CommonResponse::success)
                .map(ResponseEntity::ok);
    }
}
