package com.example.spotifyaggregator.dto;

public record SongLikeCount(
        Long songId,
        Long likeCount
) {
}
