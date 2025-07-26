-- 기존 song 테이블 제거
DROP TABLE IF EXISTS song_artist;
DROP TABLE IF EXISTS song;
DROP TABLE IF EXISTS artist;

-- song 테이블 생성
CREATE TABLE IF NOT EXISTS song (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    isrc VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    album VARCHAR(255),
    release_date DATE,
    release_year INT,
    genre VARCHAR(100),
    explicit BOOLEAN,
    popularity INT,
    UNIQUE KEY uq_song_isrc_title (isrc, title)
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_song_release_year ON song(release_year);
CREATE INDEX idx_song_release_year_album ON song(release_year, album);


-- artist 테이블 생성
CREATE TABLE IF NOT EXISTS artist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    UNIQUE KEY uq_artist_name (name)
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

-- song-artist 매핑 테이블 생성
CREATE TABLE IF NOT EXISTS song_artist (
    song_id BIGINT NOT NULL,
    artist_id BIGINT NOT NULL,
    PRIMARY KEY (song_id, artist_id),
    FOREIGN KEY (song_id) REFERENCES song(id) ON DELETE CASCADE,
    FOREIGN KEY (artist_id) REFERENCES artist(id) ON DELETE CASCADE
    ) ENGINE=InnoDB
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_song_artist_song_id ON song_artist(song_id);
CREATE INDEX idx_song_artist_artist_id ON song_artist(artist_id);