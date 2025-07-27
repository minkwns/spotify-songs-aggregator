# Spotify Aggregator

Spotify 을 기반으로 하는 노래 메타데이터를 정규화하여 DB에 적재하고, 다양한 통계 및 좋아요 기반 랭킹 정보를 제공하는 WebFlux 기반 Aggregator 프로젝트입니다.

## 주요 기능
- NDJSON 기반 대용량 곡 메타데이터 ingestion (R2DBC + Reactive Streams)
- 아티스트/연도 기반 앨범 통계 API
- 유저 기반 좋아요 등록/취소 기능 (R2DBC + Redis)
- 최근 1시간 좋아요 랭킹 Top 10 조회
- 글로벌 예외 처리 기반의 통일된 에러 응답
<br>
<br>

### 주요 로직 설명

**`SongIngestionService`는 JSON 기반 NDJSON 파일로부터 노래 및 아티스트 정보를 파싱하고, 이를 비동기 방식으로 정규화된 테이블에 저장하는 기능을 담당합니다.**

#### `SongIngestionService` 처리 흐름
1. **동시성 기반 병렬 저장 처리** : 안정적인 메모리 사용을 위해 10 개의 스레드의 동시성으로 제어합니다.
2. **중복 및 예외 처리** : 중복 키 발생 시 skip 처리하고 기타 오류 발생 시 DLQ(Dead Letter Queue)에 적재합니다.
3. **DLQ 재시도 처리**  : 실패한 Song 항목은 DLQ를 통해 최대 2회 재시도합니다.  
<br>
<br>
<br>

**`AlbumStatsService`는 노래 및 아티스트 테이블을 기반으로 연도 및 가수별 앨범 수 통계 데이터를 조회하는 기능을 담당합니다.**

#### `AlbumStatsService` 처리 흐름
- 연도별 앨범 통계 조회 : `song` 테이블의 `release_year` 기준으로 `DISTINCT album` 수를 집계하며 페이지네이션과 함께 결과 반환합니다.
- 가수별 연도별 앨범 통계 조회 : `artist`, `song_artist`, `song` 테이블을 조인하여 특정 아티스트의 연도별 앨범 개수를 계산합니다.
- 인덱스 전략 : 조회 시 효율적인 인덱싱 전략을 사용합니다.
<br>
<br>
<br>


### `SongLikeService`는 사용자의 곡 좋아요/취소/통계 기능을 Redis 및 RDB 기반으로 처리하는 서비스입니다.

#### `SongLikeService` 처리 흐름
- 좋아요 등록 : 좋아요 등록 여부를 확인한 후, RDB와 Redis 에 반영합니다. TTL 을 적용하여 반영합니다. 
- 좋아요 취소 : 기존 좋아요 여부를 확인한 후, RDB와 Redis 에 반영합니다. TTL 을 적용하여 반영합니다.
- 1시간 내 인기 Top10 조회 : 최근 60분 키 조회 → `like`는 +1, `unlike`는 -1로 집계 후 상위 10개 정렬 반환합니다.

<br>
<br>

## 기술 스택
- Java 21
- Spring Boot 3.x (WebFlux)
- Spring Data R2DBC (MySQL)
- Redis (ReactiveRedisTemplate)
- Testcontainers (Redis)
- Lombok
<br>
<br>

## 테이블 설계 (MySQL 기반 R2DBC)

곡 메타데이터, 아티스트, 좋아요 정보를 정규화된 관계형 구조로 저장합니다.

### `song` 테이블
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK) | 곡 ID |
| isrc | VARCHAR(255) | 국제 표준 녹음 코드 |
| title | VARCHAR(255) | 곡 제목 |
| album | VARCHAR(255) | 앨범명 |
| release_date | DATE | 발매일 |
| release_year | INT | 발매 연도 |
| genre | VARCHAR(100) | 장르 |
| explicit | BOOLEAN | 청불 여부 |
| popularity | INT | 인기도 |
- 복합 유니크 키: `UNIQUE(isrc, title)`
- 연도/앨범 기반 통계용 인덱스는 주석 처리됨 (`idx_song_release_year`, 등)

---

### `artist` 테이블
| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT (PK) | 아티스트 ID |
| name | VARCHAR(255) | 아티스트 이름 |
- 유니크 키: `UNIQUE(name)`

---

### `song_artist` 테이블 (N:N 매핑 테이블)
| 컬럼 | 타입 | 설명 |
|------|------|------|
| song_id | BIGINT (FK) | 곡 ID (→ song.id) |
| artist_id | BIGINT (FK) | 아티스트 ID (→ artist.id) |
- 복합 PK: `(song_id, artist_id)`
- 양쪽 모두 `ON DELETE CASCADE` 설정으로 참조 무결성 유지

---

### `song_like` 테이블
| 컬럼 | 타입 | 설명 |
|------|------|------|
| user_id | BIGINT (PK) | 사용자 ID |
| song_id | BIGINT (PK) | 곡 ID |
| liked_at | TIMESTAMP | 좋아요 누른 시간 (기본값: 현재시간) |

- 복합 PK: `(user_id, song_id)`
- 중복 좋아요 방지를 위해 유저-곡 단위로 1개만 허용
- Redis에 저장된 좋아요 기록은 해당 테이블로 적재됨
<br>
<br>

## 실행 방법

### 1. Docker로 MySQL 실행

```bash
docker run -d \
  --name spotify-mysql \
  -e MYSQL_ROOT_PASSWORD=admin1234 \
  -e MYSQL_DATABASE=spotify \
  -p 3307:3306 \
  mysql:8
```

### 2. Docker 로 Redis 실행
```bash
docker run -d \
  --name redis \
  -p 6379:6379 \
  redis
```

### 3. raw data 사용
`final_milliondataset_BERT_500K_revised.json` 파일을 `songs.json` 으로 이름 변경 후, `main/resources/data` 밑으로 붙여넣기

### 4. 빌드
```bash
./gradlew build
```

### 5. 실행
```bash
./gradlew bootRun (port 8081)
```

---
## TODO (아직 구현하지 못한 것)
- Validation
- JWT 기반 인증을 통한 User-Id 정책
- `좋아요 이력` 에 대한 로그성 데이터 적재 구간 도입 (ex. Kafka...)

### Author
- minkwns (minkwns@naver.com / junbumwhi@gmail.com)

