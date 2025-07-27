package com.example.spotifyaggregator.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    SONG_INGESTION_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "SONG_INGESTION_ERROR", "노래 정보 수집 중 오류가 발생했습니다."),
    SONG_NOT_FOUND(HttpStatus.NOT_FOUND, "SONG_NOT_FOUND", "해당 노래를 찾을 수 없습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "해당 사용자를 찾을 수 없습니다."),
    SONG_LIKE_EXISTS(HttpStatus.CONFLICT, "SONG_LIKE_EXISTS", "이미 좋아요를 누른 노래입니다."),
    SONG_LIKE_NOT_EXISTS(HttpStatus.BAD_REQUEST, "SONG_LIKE_NOT_EXISTS", "좋아요를 누르지 않은 노래입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // 앨범 관련 예외 추가
    INVALID_ARTIST_NAME(HttpStatus.BAD_REQUEST, "INVALID_ARTIST_NAME", "아티스트 이름은 필수입니다."),
    ALBUM_STATS_NOT_FOUND(HttpStatus.NOT_FOUND, "ALBUM_STATS_NOT_FOUND", "앨범 통계 데이터를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

}
