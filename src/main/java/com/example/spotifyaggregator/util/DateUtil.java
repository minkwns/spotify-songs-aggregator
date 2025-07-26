package com.example.spotifyaggregator.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Slf4j
public class DateUtil {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    public static LocalDate parseReleaseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) return null;

        try {
            // 1. 접미사 제거 (1st, 2nd, 3rd, 4th → 1, 2, 3, 4)
            String cleaned = rawDate.replaceAll("(?<=\\d)(st|nd|rd|th)", "");

            // 2. 포맷 정의
            return LocalDate.parse(cleaned, FORMATTER);
        } catch (Exception e) {
            log.warn("날짜 파싱 작업에 실패하였습니다 : {}", rawDate);
            return null;
        }
    }
}
