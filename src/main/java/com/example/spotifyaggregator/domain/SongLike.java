package com.example.spotifyaggregator.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("song_like")
public class SongLike {

    private Long userId;
    private Long songId;
    private LocalDateTime likedAt;

}
