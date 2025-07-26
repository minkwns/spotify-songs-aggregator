package com.example.spotifyaggregator.config;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class SchemaInitializer {

    private final DatabaseClient databaseClient;

    @Value("${app.schema.locations}")
    private String schemaPath;

    @PostConstruct
    public void initializeSchema() {
        try {
            // 1. 스키마 파일을 읽음
            String ddl = StreamUtils.copyToString(
                    new ClassPathResource(schemaPath).getInputStream(),
                    StandardCharsets.UTF_8
            );

            // 2. 세미콜론 기준으로 분리하고 하나씩 실행
            Arrays.stream(ddl.split(";"))
                    .map(String::trim)
                    .filter(statement -> !statement.isEmpty())
                    .forEach(sql -> {
                        log.info("SQL 스키마 실행: {}", sql);
                        databaseClient.sql(sql)
                                .then()
                                .doOnError(e -> log.error("스키마 파일 실행 실패: {}", sql, e))
                                .block();
                    });

            log.info("초기 스키마 실행 완료");
        } catch (Exception e) {
            log.error("초기 스키마 실행 실패", e);
            throw new RuntimeException("Schema initialization failed", e);
        }
    }
}
