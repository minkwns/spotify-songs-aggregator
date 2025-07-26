package com.example.spotifyaggregator.dto;

import com.example.spotifyaggregator.domain.Song;

import java.util.List;

public record SongWithArtists (
    Song song,
    List<String> artists
){}
