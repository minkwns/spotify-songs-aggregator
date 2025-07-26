package com.example.spotifyaggregator.dto;


public record AlbumStatsByArtistResponse (
        String artist,
        int releaseYear,
        long albumCount
){}
