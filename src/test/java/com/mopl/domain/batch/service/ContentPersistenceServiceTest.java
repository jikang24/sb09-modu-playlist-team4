package com.mopl.domain.batch.service;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.infra.tmdb.TmdbClient;
import com.mopl.infra.tmdb.TmdbResponse.TmdbMovieResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class ContentPersistenceServiceTest {

  @Mock
  private TmdbClient tmdbClient;

  @Mock
  private ContentRepository contentRepository;

  @Spy
  private MeterRegistry meterRegistry = new SimpleMeterRegistry();

  @InjectMocks
  private ContentPersistenceService contentPersistenceService;

  private Content makeExisting(String externalId, String thumbnailUrl) {
    return Content.restore(
        java.util.UUID.randomUUID(), ContentType.MOVIE, externalId,
        "기존 제목", "기존 설명", thumbnailUrl,
        BigDecimal.ZERO, 0,
        Instant.now(), Instant.now(), List.of()
    );
  }

  @Test
  @DisplayName("아직 저장된 적 없는 영화는 신규로 저장한다")
  void saveMovies_new_movie_is_inserted() {
    TmdbMovieResult movie = new TmdbMovieResult(
        1L, "새 영화", "설명", "/poster.jpg", List.of(), "2026-01-01", 8.0);
    given(contentRepository.findAllByType(ContentType.MOVIE)).willReturn(List.of());
    given(tmdbClient.buildImageUrl("/poster.jpg")).willReturn("https://image.tmdb.org/t/p/w500/poster.jpg");

    int savedCount = contentPersistenceService.saveMovies(List.of(movie));

    assertThat(savedCount).isEqualTo(1);
    ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
    then(contentRepository).should().save(captor.capture());
    assertThat(captor.getValue().getThumbnailUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
    assertThat(meterRegistry.counter("content.sync.saved", "type", ContentType.MOVIE.name()).count())
        .isEqualTo(1.0);
  }

  @Test
  @DisplayName("이미 저장돼 있지만 썸네일이 비어있던 영화는 새로 받은 썸네일로 갱신한다")
  void saveMovies_fills_missing_thumbnail_on_existing_content() {
    TmdbMovieResult movie = new TmdbMovieResult(
        1L, "기존 제목", "설명", "/poster.jpg", List.of(), "2026-01-01", 8.0);
    Content existing = makeExisting("1", null);
    given(contentRepository.findAllByType(ContentType.MOVIE)).willReturn(List.of(existing));
    given(tmdbClient.buildImageUrl("/poster.jpg")).willReturn("https://image.tmdb.org/t/p/w500/poster.jpg");

    int savedCount = contentPersistenceService.saveMovies(List.of(movie));

    assertThat(savedCount).isEqualTo(0); // 신규 저장 건수는 늘지 않음 (갱신이므로)
    then(contentRepository).should().save(existing);
    assertThat(existing.getThumbnailUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/poster.jpg");
    assertThat(meterRegistry.counter("content.sync.saved", "type", ContentType.MOVIE.name()).count())
        .isEqualTo(0.0); // 갱신은 신규 저장 메트릭에 반영되지 않음
  }

  @Test
  @DisplayName("이미 썸네일이 채워진 영화는 값이 바뀌어도 다시 저장하지 않는다")
  void saveMovies_does_not_touch_content_that_already_has_thumbnail() {
    TmdbMovieResult movie = new TmdbMovieResult(
        1L, "기존 제목", "설명", "/new-poster.jpg", List.of(), "2026-01-01", 8.0);
    Content existing = makeExisting("1", "https://image.tmdb.org/t/p/w500/old-poster.jpg");
    given(contentRepository.findAllByType(ContentType.MOVIE)).willReturn(List.of(existing));
    given(tmdbClient.buildImageUrl("/new-poster.jpg")).willReturn("https://image.tmdb.org/t/p/w500/new-poster.jpg");

    int savedCount = contentPersistenceService.saveMovies(List.of(movie));

    assertThat(savedCount).isEqualTo(0);
    assertThat(existing.getThumbnailUrl()).isEqualTo("https://image.tmdb.org/t/p/w500/old-poster.jpg");
    then(contentRepository).should(never()).save(any());
  }

  @Test
  @DisplayName("장르 ID가 있으면 장르 이름으로 변환해 태그로 저장한다")
  void saveMovies_mapsGenreIdsToTagNames() {
    TmdbMovieResult movie = new TmdbMovieResult(
        1L, "새 영화", "설명", "/poster.jpg", List.of(28, 12), "2026-01-01", 8.0);
    given(contentRepository.findAllByType(ContentType.MOVIE)).willReturn(List.of());
    given(tmdbClient.buildImageUrl("/poster.jpg")).willReturn("https://image.tmdb.org/t/p/w500/poster.jpg");
    given(tmdbClient.getMovieGenres()).willReturn(Map.of(28, "액션", 12, "모험"));

    contentPersistenceService.saveMovies(List.of(movie));

    ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
    then(contentRepository).should().save(captor.capture());
    assertThat(captor.getValue().getTags()).containsExactlyInAnyOrder("액션", "모험");
  }

  @Test
  @DisplayName("매핑에 없는 장르 id는 건너뛰고 나머지만 태그로 저장한다")
  void saveMovies_skipsUnknownGenreIds() {
    TmdbMovieResult movie = new TmdbMovieResult(
        1L, "새 영화", "설명", "/poster.jpg", List.of(28, 9999), "2026-01-01", 8.0);
    given(contentRepository.findAllByType(ContentType.MOVIE)).willReturn(List.of());
    given(tmdbClient.buildImageUrl("/poster.jpg")).willReturn("https://image.tmdb.org/t/p/w500/poster.jpg");
    given(tmdbClient.getMovieGenres()).willReturn(Map.of(28, "액션"));

    contentPersistenceService.saveMovies(List.of(movie));

    ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
    then(contentRepository).should().save(captor.capture());
    assertThat(captor.getValue().getTags()).containsExactly("액션");
  }

  @Test
  @DisplayName("장르 목록 조회가 실패해도 영화 저장 자체는 계속 진행된다")
  void saveMovies_continuesWhenGenreLookupFails() {
    TmdbMovieResult movie = new TmdbMovieResult(
        1L, "새 영화", "설명", "/poster.jpg", List.of(28), "2026-01-01", 8.0);
    given(contentRepository.findAllByType(ContentType.MOVIE)).willReturn(List.of());
    given(tmdbClient.buildImageUrl("/poster.jpg")).willReturn("https://image.tmdb.org/t/p/w500/poster.jpg");
    given(tmdbClient.getMovieGenres()).willThrow(new RuntimeException("TMDB 장애"));

    int savedCount = contentPersistenceService.saveMovies(List.of(movie));

    assertThat(savedCount).isEqualTo(1);
    ArgumentCaptor<Content> captor = ArgumentCaptor.forClass(Content.class);
    then(contentRepository).should().save(captor.capture());
    assertThat(captor.getValue().getTags()).isEmpty();
  }
}