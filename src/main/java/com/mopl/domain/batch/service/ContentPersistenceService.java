package com.mopl.domain.batch.service;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.infra.sportsdb.SportsDbClient.SportsDbEvent;
import com.mopl.infra.tmdb.TmdbClient;
import com.mopl.infra.tmdb.TmdbResponse.TmdbMovieResult;
import com.mopl.infra.tmdb.TmdbResponse.TmdbTvResult;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 콘텐츠 수집 결과 DB 저장 담당
 *
 * ContentSyncService에서 이 빈의 메서드를 호출하는 형태로 분리되어 있어야
 * @Transactional이 Spring AOP 프록시를 거쳐 정상적으로 적용된다.
 * (같은 클래스 안에서 this.saveMovies()처럼 자기 자신을 호출하면 프록시를 우회해
 * 트랜잭션이 걸리지 않고, findExistingByExternalId 이후 지연 로딩되는 tags 컬렉션 접근 시
 * 세션이 이미 닫혀 있어 LazyInitializationException이 발생한다)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentPersistenceService {

  private final TmdbClient tmdbClient;
  private final ContentRepository contentRepository;
  private final MeterRegistry meterRegistry;

  @Transactional
  public int saveMovies(List<TmdbMovieResult> movies) {
    // 기존 저장된 MOVIE를 externalId 기준으로 한번에 조회 (쿼리 1번, N+1 방지)
    Map<String, Content> existingByExternalId = findExistingByExternalId(ContentType.MOVIE);
    Map<Integer, String> genreNamesById = fetchGenresSafely(tmdbClient::getMovieGenres, "영화");
    int savedCount = 0;

    for (TmdbMovieResult movie : movies) {
      try {
        String externalId = String.valueOf(movie.id());
        Content existing = existingByExternalId.get(externalId);
        String thumbnailUrl = tmdbClient.buildImageUrl(movie.posterPath());

        if (existing == null) {
          Content content = Content.create(
              ContentType.MOVIE,
              externalId,
              movie.title(),
              movie.overview(),
              thumbnailUrl,
              toGenreTags(movie.genreIds(), genreNamesById)
          );
          contentRepository.save(content);
          savedCount++;
          meterRegistry.counter("content.sync.saved", "type", ContentType.MOVIE.name()).increment();
        } else {
          // 신규 수집 시점엔 TMDB가 poster_path를 안 내려줘서 썸네일이 비어있던 콘텐츠 보정
          fillMissingThumbnail(existing, thumbnailUrl);
        }
      } catch (Exception e) {
        log.warn("[Batch] 영화 저장 실패 - id: {}, 원인: {}", movie.id(), e.getMessage());
      }
    }
    log.info("[Batch] 영화 수집 완료 - 저장: {}건", savedCount);
    return savedCount;
  }

  @Transactional
  public int saveDramas(List<TmdbTvResult> tvSeries) {
    // 기존 저장된 TV_SERIES를 externalId 기준으로 한번에 조회 (쿼리 1번, N+1 방지)
    Map<String, Content> existingByExternalId = findExistingByExternalId(ContentType.TV_SERIES);
    Map<Integer, String> genreNamesById = fetchGenresSafely(tmdbClient::getTvGenres, "TV시리즈");
    int savedCount = 0;

    for (TmdbTvResult drama : tvSeries) {
      try {
        String externalId = String.valueOf(drama.id());
        Content existing = existingByExternalId.get(externalId);
        String thumbnailUrl = tmdbClient.buildImageUrl(drama.posterPath());

        if (existing == null) {
          Content content = Content.create(
              ContentType.TV_SERIES,
              externalId,
              drama.name(),
              drama.overview(),
              thumbnailUrl,
              toGenreTags(drama.genreIds(), genreNamesById)
          );
          contentRepository.save(content);
          savedCount++;
          meterRegistry.counter("content.sync.saved", "type", ContentType.TV_SERIES.name()).increment();
        } else {
          fillMissingThumbnail(existing, thumbnailUrl);
        }
      } catch (Exception e) {
        log.warn("[Batch] TV시리즈 저장 실패 - id: {}, 원인: {}", drama.id(), e.getMessage());
      }
    }
    log.info("[Batch] TV시리즈 수집 완료 -  저장: {}건", savedCount);
    return savedCount;
  }

  @Transactional
  public int saveSportsEvents(List<SportsDbEvent> events) {
    // 기존 저장된 SPORT externalId를 한번에 조회 (쿼리 1번)
    Set<String> existingIds = contentRepository.findExternalIdsByType(ContentType.SPORT);
    int savedCount = 0;

    for (SportsDbEvent event : events) {
      if (existingIds.contains(event.idEvent())) continue;
      try {
        String title = event.strHomeTeam() + " vs " + event.strAwayTeam();
        String description = event.strLeague() + " | " + event.dateEvent();

        Content content = Content.create(
            ContentType.SPORT,
            event.idEvent(),
            title,
            description,
            event.resolveThumbnailUrl(),
            List.of(event.strLeague())
        );
        contentRepository.save(content);
        savedCount++;
        meterRegistry.counter("content.sync.saved", "type", ContentType.SPORT.name()).increment();
      } catch (Exception e) {
        log.warn("[Batch] 스포츠 저장 실패 - id: {}, 원인: {}", event.idEvent(), e.getMessage());
      }
    }
    log.info("[Batch] 스포츠 수집 완료 - 저장: {}건", savedCount);
    return savedCount;
  }

  /** 타입별 기존 콘텐츠를 externalId 기준 Map으로 조회 (신규/보정 대상 판별용, 쿼리 1번) */
  private Map<String, Content> findExistingByExternalId(ContentType type) {
    return contentRepository.findAllByType(type).stream()
        .collect(Collectors.toMap(Content::getExternalId, Function.identity()));
  }

  /** 장르 목록 조회 실패가 영화/TV 수집 전체를 막지 않도록 방어 (실패 시 태그 없이 진행) */
  private Map<Integer, String> fetchGenresSafely(
      Supplier<Map<Integer, String>> genreSupplier, String typeName) {
    try {
      return genreSupplier.get();
    } catch (Exception e) {
      log.warn("[Batch] {} 장르 목록 조회 실패 - 태그 없이 진행: {}", typeName, e.getMessage());
      return Map.of();
    }
  }

  /** TMDB genre_ids를 실제 장르 이름 태그로 변환 (매핑에 없는 id는 건너뜀) */
  private List<String> toGenreTags(List<Integer> genreIds, Map<Integer, String> genreNamesById) {
    if (genreIds == null || genreIds.isEmpty() || genreNamesById == null) {
      return List.of();
    }
    return genreIds.stream()
        .map(genreNamesById::get)
        .filter(Objects::nonNull)
        .toList();
  }

  /**
   * 최초 수집 시점에 TMDB가 poster_path를 내려주지 않아 thumbnailUrl이 비어있던 콘텐츠를
   * 이후 배치 회차에서 값이 채워지면 갱신한다. (신규 URL이 없거나 이미 채워져 있으면 스킵)
   */
  private void fillMissingThumbnail(Content existing, String thumbnailUrl) {
    if (existing.getThumbnailUrl() != null || thumbnailUrl == null) {
      return;
    }
    existing.update(existing.getType(), existing.getTitle(), existing.getDescription(),
        thumbnailUrl, existing.getTags());
    contentRepository.save(existing);
    log.info("[Batch] 썸네일 보정 완료 - externalId: {}, title: {}", existing.getExternalId(), existing.getTitle());
  }
}
