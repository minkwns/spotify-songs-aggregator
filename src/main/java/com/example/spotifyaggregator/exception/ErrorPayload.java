package com.example.spotifyaggregator.exception;

import lombok.Builder;

public record ErrorPayload(
        int httpStatus,
        String errorCode,
        String message,
        String path
) {
    @Builder
    public ErrorPayload {}
}
