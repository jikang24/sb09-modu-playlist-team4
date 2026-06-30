package com.mopl.domain.batch.service;

import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.infra.sportsdb.SportsDbClient;
import com.mopl.infra.sportsdb.SportsDbClient.SportsDbEvent;
import com.mopl.infra.tmdb.TmdbClient;
import com.mopl.infra.tmdb.TmdbResponse.TmdbMovieResult;
import com.mopl.infra.tmdb.TmdbResponse.TmdbTvResult;
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


  @Transactional
  public int syncMovies(int page) {
    List<TmdbMovieResult> movies = tmdbClient.fetchPopularMovies(page);
    int savedCount = 0;

    for (TmdbMovieResult movie : movies) {
      try {
        // 이미 수집된 콘텐츠면 건너뜀 (UNIQUE 제약 활용)
        boolean exists = contentRepository
            .findByTypeAndExternalId(ContentType.MOVIE, String.valueOf(movie.id()))
            .isPresent();

        if (!exists) {
          Content content = Content.create(
              ContentType.MOVIE,
              String.valueOf(movie.id()),
              movie.title(),
              movie.overview(),
              tmdbClient.buildImageUrl(movie.posterPath()),
              List.of() // TODO 태그는 장르 API 별도 호출 필요 → 추후 추가
          );
          contentRepository.save(content);
          savedCount++;
        }
      } catch (Exception e) {
        // 단건 실패 시 전체 중단 X → 로그 남기고 계속
        log.warn("[Batch] 영화 저장 실패 - id: {}, 원인: {}", movie.id(), e.getMessage());
      }
    }

    log.info("[Batch] 영화 수집 완료 - page: {}, 저장: {}건", page, savedCount);
    return savedCount;
  }

  @Transactional
  public int syncTvSeries(int page) {
    List<TmdbTvResult> serieses = tmdbClient.fetchPopularTv(page);
    int savedCount = 0;

    for (TmdbTvResult series : serieses) {
      try {
        boolean exists = contentRepository
            .findByTypeAndExternalId(ContentType.TV_SERIES, String.valueOf(series.id()))
            .isPresent();

        if (!exists) {
          Content content = Content.create(
              ContentType.TV_SERIES,
              String.valueOf(series.id()),
              series.name(), // TV는 name 필드
              series.overview(),
              tmdbClient.buildImageUrl(series.posterPath()),
              List.of()
          );
          contentRepository.save(content);
          savedCount++;
        }
      } catch (Exception e) {
        log.warn("[Batch] TV시리즈 저장 실패 - id: {}, 원인: {}", series.id(), e.getMessage());
      }
    }

    log.info("[Batch] TV시리즈 수집 완료 - page: {}, 저장: {}건", page, savedCount);
    return savedCount;
  }

  @Transactional
  public int syncSportsEvents() {
    List<SportsDbEvent> events = sportsDbClient.fetchEplEvents();
    int savedCount = 0;

    for (SportsDbEvent event : events) {
      try {
        boolean exists = contentRepository
            .findByTypeAndExternalId(ContentType.SPORT, event.idEvent())
            .isPresent();

        if (!exists) {
          // 경기 제목: "홈팀 vs 원정팀" 형식
          String title = event.strHomeTeam() + " vs " + event.strAwayTeam();
          // 경기 설명: 리그명 + 날짜
          String description = event.strLeague() + " | " + event.dateEvent();

          Content content = Content.create(
              ContentType.SPORT,
              event.idEvent(),
              title,
              description,
              event.strThumb(),
              List.of(event.strLeague()) // 리그명을 태그로
          );
          contentRepository.save(content);
          savedCount++;
        }
      } catch (Exception e) {
        log.warn("[Batch] 경기 저장 실패 - id: {}, 원인: {}", event.idEvent(), e.getMessage());
      }
    }

    log.info("[Batch] 스포츠 수집 완료 - 저장: {}건", savedCount);
    return savedCount;
  }
}