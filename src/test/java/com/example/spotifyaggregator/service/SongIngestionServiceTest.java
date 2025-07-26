package com.example.spotifyaggregator.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import reactor.test.StepVerifier;

@SpringBootTest
class SongIngestionServiceTest {

    @Autowired
    private SongIngestionService songIngestionService;

    @Test
    @DisplayName("테스트 파일을 정상적으로 파싱하고 저장한다")
    void testIngest() {
        StepVerifier.create(songIngestionService.ingestFromJsonFile("data/songs_test.json"))
                .expectNextMatches(result -> {
                    System.out.println("성공: " + result.successCount());
                    System.out.println("실패: " + result.failureCount());
                    return result.successCount() > 0;
                })
                .verifyComplete();
    }
}