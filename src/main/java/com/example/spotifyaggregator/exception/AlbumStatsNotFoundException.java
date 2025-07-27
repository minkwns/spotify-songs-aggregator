package com.example.spotifyaggregator.exception;

public class AlbumStatsNotFoundException extends AlbumStatsException {
    public AlbumStatsNotFoundException() {
        super(ErrorCode.ALBUM_STATS_NOT_FOUND);
    }
}
