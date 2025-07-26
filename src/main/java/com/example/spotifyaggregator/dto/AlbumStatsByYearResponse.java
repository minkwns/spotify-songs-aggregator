package com.example.spotifyaggregator.dto;


public record AlbumStatsByYearResponse (
        int releaseYear,
        long albumCount
){}
