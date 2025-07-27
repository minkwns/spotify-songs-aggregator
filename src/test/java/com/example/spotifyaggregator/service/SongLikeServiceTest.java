package com.example.spotifyaggregator.service;

import com.example.spotifyaggregator.dto.SongLikeAck;
import com.example.spotifyaggregator.exception.ErrorCode;
import com.example.spotifyaggregator.exception.SongLikeException;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@Slf4j
@ActiveProfiles("test")
@Testcontainers
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SongLikeServiceTest {

    @Container
    static final GenericContainer<?> redis = new GenericContainer<>("redis:7.0.0")
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort())
            .withStartupTimeout(Duration.ofSeconds(10));

    @DynamicPropertySource
    static void overrideRedisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private SongLikeService songLikeService;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private final Long songId1 = 777L;
    private final Long songId2 = 888L;

    @Test
    @Order(1)
    void 좋아요_등록_Redis_RDB_검증() {
        log.info("좋아요 등록 테스트 시작");
        Mono<Void> testFlow = songLikeService.likeSong(songId1, 1L)
                .then(songLikeService.likeSong(songId1, 2L))
                .then(songLikeService.likeSong(songId2, 3L))
                .then();

        StepVerifier.create(testFlow)
                .verifyComplete();
    }

    @Test
    @Order(2)
    void 중복_좋아요_방지() {
        log.info("좋아요 중복 방지 테스트 시작");

        Mono<SongLikeAck> result = songLikeService.likeSong(777L, 777L);

        StepVerifier.create(result)
                .assertNext(ack -> {
                    log.info("ACK 응답 확인: {}", ack);
                    assertThat(ack.songId()).isEqualTo(777L);  // 예상 값과 동일한지 확인
                    assertThat(ack.message()).contains("좋아요가 반영되었습니다");
                })
                .verifyComplete();
    }

    @Test
    @Order(3)
    void 좋아요_취소_정상처리() {
        log.info("좋아요 취소 테스트 시작");

        Long userId = 123L;
        Long songId = 456L;

        // 좋아요 먼저 누름
        Mono<Void> init = songLikeService.likeSong(songId, userId).then();

        // 좋아요 취소
        Mono<SongLikeAck> result = init.then(songLikeService.unlikeSong(songId, userId));

        StepVerifier.create(result)
                .assertNext(ack -> {
                    log.info("응답: {}", ack);
                    assertThat(ack.songId()).isEqualTo(songId);
                    assertThat(ack.message()).contains("좋아요가 취소되었습니다.");
                })
                .verifyComplete();
    }

    @Test
    @Order(4)
    void 좋아요_취소_실패_비정상케이스() {
        log.info("좋아요 취소 실패 테스트 시작");

        Long userId = 999L;
        Long songId = 888L;

        Mono<SongLikeAck> result = songLikeService.unlikeSong(songId, userId);

        StepVerifier.create(result)
                .expectErrorSatisfies(error -> {
                    assertThat(error).isInstanceOf(SongLikeException.class);
                    SongLikeException ex = (SongLikeException) error;
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.SONG_LIKE_NOT_EXISTS);
                })
                .verify();
    }
}