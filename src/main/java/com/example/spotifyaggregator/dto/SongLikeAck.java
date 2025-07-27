package com.example.spotifyaggregator.dto;

public record SongLikeAck(
        Long songId,
        String message
) {
}
