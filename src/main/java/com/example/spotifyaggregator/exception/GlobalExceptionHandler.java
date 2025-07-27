package com.example.spotifyaggregator.exception;

import com.example.spotifyaggregator.dto.CommonResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;


@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(SongIngestionException.class)
    public Mono<ResponseEntity<CommonResponse<ErrorPayload>>> handleSongIngestionException(SongIngestionException e) {
        log.warn("SongIngestionException 발생: {}", e.getMessage(), e);

        return Mono.just(
                ResponseEntity
                        .status(e.getHttpStatus())
                        .body(CommonResponse.error(
                                e.getErrorCode(),
                                ErrorPayload.builder()
                                        .httpStatus(e.getHttpStatus().value())
                                        .errorCode(e.getErrorCode().name())
                                        .message(e.getErrorCode().getMessage())
                                        .path("/api/songs/ingest")  // 추후 ServerRequest로 교체 가능
                                        .build()
                        ))
        );
    }

    @ExceptionHandler(AlbumStatsException.class)
    public Mono<ResponseEntity<CommonResponse<ErrorPayload>>> handleAlbumStatsException(AlbumStatsException e) {
        log.warn("AlbumStatsException 발생: {}", e.getMessage(), e);

        return Mono.just(
                ResponseEntity
                        .status(e.getHttpStatus())
                        .body(CommonResponse.error(
                                e.getErrorCode(),
                                ErrorPayload.builder()
                                        .httpStatus(e.getHttpStatus().value())
                                        .errorCode(e.getErrorCode().getCode())
                                        .message(e.getErrorCode().getMessage())
                                        .path("/api/albums")
                                        .build()
                        ))
        );
    }

    @ExceptionHandler(SongLikeException.class)
    public Mono<ResponseEntity<CommonResponse<ErrorPayload>>> handleSongLikeException(SongLikeException e) {
        log.warn("SongLikeException 발생: {}", e.getMessage(), e);

        return Mono.just(
                ResponseEntity
                        .status(e.getHttpStatus())
                        .body(CommonResponse.error(
                                e.getErrorCode(),
                                ErrorPayload.builder()
                                        .httpStatus(e.getHttpStatus().value())
                                        .errorCode(e.getErrorCode().getCode())
                                        .message(e.getErrorCode().getMessage())
                                        .path("/api/songs/{songId}/like")  // 추후 path 개선 예정
                                        .build()
                        ))
        );
    }


    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<CommonResponse<ErrorPayload>>> handleGenericException(Exception e) {
        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(CommonResponse.error(
                                ErrorCode.INTERNAL_SERVER_ERROR,
                                ErrorPayload.builder()
                                        .httpStatus(HttpStatus.INTERNAL_SERVER_ERROR.value())
                                        .errorCode(ErrorCode.INTERNAL_SERVER_ERROR.name())
                                        .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
                                        .path("unknown") // 또는 RequestContextHolder 등으로 추후 개선 가능
                                        .build()
                        ))
        );
    }

}
