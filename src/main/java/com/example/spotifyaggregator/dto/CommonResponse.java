package com.example.spotifyaggregator.dto;

import org.springframework.http.HttpStatus;
import com.example.spotifyaggregator.exception.ErrorCode;

public record CommonResponse<T>(
        boolean success,
        String code,
        String message,
        T payload
) {
    public static <T> CommonResponse<T> success(T payload) {
        return new CommonResponse<>(true, "SUCCESS", "정상 처리되었습니다.", payload);
    }

    public static <T> CommonResponse<T> failure(String code, String message) {
        return new CommonResponse<>(false, code, message, null);
    }

    public static <T> CommonResponse<T> error(ErrorCode errorCode, T payload) {
        return new CommonResponse<>(false, errorCode.getCode(), errorCode.getMessage(), payload);
    }
}

