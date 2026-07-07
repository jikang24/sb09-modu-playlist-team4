package com.mopl.domain.batch.service;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.infra.sportsdb.SportsDbClient;
import com.mopl.infra.sportsdb.SportsDbClient.SportsDbEvent;
import com.mopl.infra.tmdb.TmdbClient;
import com.mopl.infra.tmdb.TmdbResponse.TmdbMovieResult;
import com.mopl.infra.tmdb.TmdbResponse.TmdbTvResult;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 콘텐츠 수집 서비스
 *
 * Batch Job의 Step에서 호출됨
 * 외부 API 응답 → Content 도메인 변환 → 저장
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSyncService {

  private final TmdbClient tmdbClient;
  private final SportsDbClient sportsDbClient;
  private final ContentRepository contentRepository;


  public int syncMovies(int page) {
    // 1. 외부 API 호출 (트랜잭션 밖 - 네트워크 지연이 DB 커넥션에 영향 없음)
    List<TmdbMovieResult> movies = tmdbClient.fetchPopularMovies(page);
    log.info("[Batch] 영화 API 응답 완료 - page: {}, 건수: {}", page, movies.size());

    // 2. DB 저장 (트랜잭션 시작 - 짧게 유지)
    return saveMovies(movies);
  }

  @Transactional
  public int saveMovies(List<TmdbMovieResult> movies) {
    // 기존 저장된 MOVIE를 externalId 기준으로 한번에 조회 (쿼리 1번, N+1 방지)
    Map<String, Content> existingByExternalId = findExistingByExternalId(ContentType.MOVIE);
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
              List.of()// TODO 태그는 장르 API 별도 호출 필요 → 추후 추가
          );
          contentRepository.save(content);
          savedCount++;
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


  public int syncTvSeries(int page) {
    // 1. 외부 API 호출 (트랜잭션 밖)
    List<TmdbTvResult> tvSeries = tmdbClient.fetchPopularTv(page);
    log.info("[Batch] TV시리즈 API 응답 완료 - page: {}, 건수: {}", page, tvSeries.size());

    // 2. DB 저장 (트랜잭션 시작)
    return saveDramas(tvSeries);
  }

  @Transactional
  public int saveDramas(List<TmdbTvResult> tvSeries) {
    // 기존 저장된 TV_SERIES를 externalId 기준으로 한번에 조회 (쿼리 1번, N+1 방지)
    Map<String, Content> existingByExternalId = findExistingByExternalId(ContentType.TV_SERIES);
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
              List.of()
          );
          contentRepository.save(content);
          savedCount++;
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

  public int syncSportsEvents() {
    // 1. 외부 API 호출 (트랜잭션 밖)
    List<SportsDbEvent> events = sportsDbClient.fetchEplEvents();
    log.info("[Batch] 스포츠 API 응답 완료 - 건수: {}", events.size());

    // 2. DB 저장 (트랜잭션 시작)
    return saveSportsEvents(events);
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
            event.strThumb(),
            List.of(event.strLeague())
        );
        contentRepository.save(content);
        savedCount++;
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

  /**
   * 최초 수집 시점에 TMDB가 poster_path를 내려주지 않아 thumbnailUrl이 비어있던 콘텐츠를
   * 이후 배치 회차에서 값이 채워지면 갱신한다. (신규 URL이 없거나 이미 채워져 있으면 스킵)
   */
  private void fillMissingThumbnail(Content existing, String thumbnailUrl) {
    if (existing.getThumbnailUrl() != null || thumbnailUrl == null) {
      return;
    }
    existing.update(existing.getTitle(), existing.getDescription(), thumbnailUrl, existing.getTags());
    contentRepository.save(existing);
    log.info("[Batch] 썸네일 보정 완료 - externalId: {}, title: {}", existing.getExternalId(), existing.getTitle());
  }

}