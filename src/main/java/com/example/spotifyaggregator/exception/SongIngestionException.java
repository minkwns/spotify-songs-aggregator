package com.example.spotifyaggregator.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class SongIngestionException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public SongIngestionException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.httpStatus = errorCode.getHttpStatus();
    }
}
