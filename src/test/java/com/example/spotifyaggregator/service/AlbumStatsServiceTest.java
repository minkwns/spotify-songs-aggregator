package com.example.spotifyaggregator.service;

import com.example.spotifyaggregator.domain.Artist;
import com.example.spotifyaggregator.domain.Song;
import com.example.spotifyaggregator.domain.SongArtist;
import com.example.spotifyaggregator.dto.AlbumStatsByArtistResponse;
import com.example.spotifyaggregator.dto.AlbumStatsByYearResponse;
import com.example.spotifyaggregator.repository.ArtistRepository;
import com.example.spotifyaggregator.repository.SongArtistRepository;
import com.example.spotifyaggregator.repository.SongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDate;

@SpringBootTest
@ActiveProfiles("test")
class AlbumStatsServiceTest {

    @Autowired private AlbumStatsService albumStatsService;
    @Autowired private SongRepository songRepository;
    @Autowired private ArtistRepository artistRepository;
    @Autowired private SongArtistRepository songArtistRepository;

    private Long artistAId;
    private Long artistBId;

    @BeforeEach
    void setUp() {
        songArtistRepository.deleteAll().block();
        songRepository.deleteAll().block();
        artistRepository.deleteAll().block();

        // 더미 데이터 삽입
        Song song1 = Song.builder().isrc("ISRC001").title("Hello1").album("ALB1").releaseDate(LocalDate.of(2021, 5, 1)).releaseYear(2021).build();
        Song song2 = Song.builder().isrc("ISRC002").title("Hello2").album("ALB2").releaseDate(LocalDate.of(2021, 6, 1)).releaseYear(2021).build();
        Song song3 = Song.builder().isrc("ISRC003").title("Hello3").album("ALB3").releaseDate(LocalDate.of(2022, 7, 1)).releaseYear(2022).build();

        Artist artistA = Artist.builder().name("ArtistA").build();
        Artist artistB = Artist.builder().name("ArtistB").build();

        StepVerifier.create(
                artistRepository.saveAll(Flux.just(artistA, artistB)).collectList()
                        .flatMap(artists -> {
                            artistAId = artists.get(0).getId();
                            artistBId = artists.get(1).getId();

                            return songRepository.saveAll(Flux.just(song1, song2, song3)).collectList()
                                    .flatMap(songs -> {
                                        Song s1 = songs.get(0);
                                        Song s2 = songs.get(1);
                                        Song s3 = songs.get(2);

                                        return songArtistRepository.saveAll(Flux.just(
                                                SongArtist.builder().songId(s1.getId()).artistId(artistAId).build(),
                                                SongArtist.builder().songId(s2.getId()).artistId(artistAId).build(),
                                                SongArtist.builder().songId(s3.getId()).artistId(artistBId).build()
                                        )).then();
                                    });
                        })
        ).verifyComplete();
    }

    @Test
    @DisplayName("연도별 앨범 통계가 정상적으로 조회되어야 한다.")
    void testGetAlbumStatsByYear() {
        StepVerifier.create(albumStatsService.getAlbumStatsByYear(0, 10))
                .assertNext(response -> {
                    assert response.content().size() == 2;

                    AlbumStatsByYearResponse stat2021 = response.content().stream()
                            .filter(r -> r.releaseYear() == 2021)
                            .findFirst().orElseThrow();
                    assert stat2021.albumCount() == 2;

                    AlbumStatsByYearResponse stat2022 = response.content().stream()
                            .filter(r -> r.releaseYear() == 2022)
                            .findFirst().orElseThrow();
                    assert stat2022.albumCount() == 1;
                })
                .verifyComplete();

    }

    @Test
    @DisplayName("가수별 연도 및 앨범 통계가 정상적으로 조회되어야 한다.")
    void testGetAlbumStatsByArtist() {
        StepVerifier.create(albumStatsService.getAlbumStatsByArtist("ArtistA", 0, 10))
                .assertNext(response -> {
                    assert response.content().size() == 1;
                    AlbumStatsByArtistResponse stat = response.content().get(0);
                    assert stat.artist().equals("ArtistA");
                    assert stat.releaseYear() == 2021;
                    assert stat.albumCount() == 2;
                })
                .verifyComplete();
    }
}