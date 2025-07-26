package com.example.spotifyaggregator.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Table("song")
public class Song {

    @Id
    private Long id;
    private String isrc; // International Standard Recording Code
    private String title;
    private String album;
    private LocalDate releaseDate;
    private String genre;
    private Boolean explicit;
    private Integer popularity;

}
