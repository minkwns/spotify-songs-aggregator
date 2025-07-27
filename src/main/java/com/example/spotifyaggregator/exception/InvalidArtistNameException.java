package com.example.spotifyaggregator.exception;

public class InvalidArtistNameException extends AlbumStatsException {
    public InvalidArtistNameException() {
        super(ErrorCode.INVALID_ARTIST_NAME);
    }
}
