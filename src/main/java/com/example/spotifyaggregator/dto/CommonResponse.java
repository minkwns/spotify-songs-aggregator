package com.example.spotifyaggregator.dto;

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
}

