package com.example.spotifyaggregator.dto;

public record SongIngestionResult(
        int successCount,
        int failureCount
) {
}
