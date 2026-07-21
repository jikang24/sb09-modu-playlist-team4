package com.mopl.domain.batch.service;

import com.mopl.infra.sportsdb.SportsDbClient;
import com.mopl.infra.sportsdb.SportsDbClient.SportsDbEvent;
import com.mopl.infra.tmdb.TmdbClient;
import com.mopl.infra.tmdb.TmdbResponse.TmdbMovieResult;
import com.mopl.infra.tmdb.TmdbResponse.TmdbTvResult;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 콘텐츠 수집 서비스
 *
 * Batch Job의 Step에서 호출됨
 * 외부 API 호출은 여기서, DB 저장(트랜잭션)은 ContentPersistenceService에 위임
 * (같은 클래스 안에서 저장 메서드를 직접 호출하면 @Transactional 프록시가 우회되므로
 * 반드시 별도 빈으로 분리해 호출하도록 변경)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ContentSyncService {

  private final TmdbClient tmdbClient;
  private final SportsDbClient sportsDbClient;
  private final ContentPersistenceService contentPersistenceService;

  public int syncMovies(int page) {
    // 1. 외부 API 호출 (트랜잭션 밖 - 네트워크 지연이 DB 커넥션에 영향 없음)
    List<TmdbMovieResult> movies = tmdbClient.fetchPopularMovies(page);
    log.info("[Batch] 영화 API 응답 완료 - page: {}, 건수: {}", page, movies.size());

    // 2. DB 저장 (트랜잭션 시작 - 짧게 유지)
    return contentPersistenceService.saveMovies(movies);
  }

  public int syncTvSeries(int page) {
    // 1. 외부 API 호출 (트랜잭션 밖)
    List<TmdbTvResult> tvSeries = tmdbClient.fetchPopularTv(page);
    log.info("[Batch] TV시리즈 API 응답 완료 - page: {}, 건수: {}", page, tvSeries.size());

    // 2. DB 저장 (트랜잭션 시작)
    return contentPersistenceService.saveDramas(tvSeries);
  }

  public int syncSportsEvents() {
    // 1. 외부 API 호출 (트랜잭션 밖)
    List<SportsDbEvent> events = sportsDbClient.fetchEplEvents();
    log.info("[Batch] 스포츠 API 응답 완료 - 건수: {}", events.size());

    // 2. DB 저장 (트랜잭션 시작)
    return contentPersistenceService.saveSportsEvents(events);
  }
}
