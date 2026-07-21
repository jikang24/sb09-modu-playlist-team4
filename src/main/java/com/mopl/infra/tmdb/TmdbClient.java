package com.mopl.infra.tmdb;

import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.infra.common.ExternalApiRetryableException;
import com.mopl.infra.tmdb.TmdbResponse.TmdbGenreListResponse;
import com.mopl.infra.tmdb.TmdbResponse.TmdbMovieResult;
import com.mopl.infra.tmdb.TmdbResponse.TmdbPageResponse;
import com.mopl.infra.tmdb.TmdbResponse.TmdbTvResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * TMDB API 클라이언트
 *
 * RestClient: RestTemplate 대체, 요청조립 등에서 이점(동기 방식)
 * 나중에 병목 지점이 보이면 그 부분만 WebClient로 교체하는 식 고려
 *
 * 인증 방식: Bearer Token (API 읽기 액세스 토큰)
 * 429/5xx는 externalApiRetryTemplate으로 지수 백오프 재시도, 그 외 4xx는 즉시 실패 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbClient {

  private final TmdbProperties tmdbProperties;
  private final RetryTemplate externalApiRetryTemplate;

  // 장르 목록은 거의 바뀌지 않는 정적 데이터라 최초 조회 후 메모리에 캐싱해서 재사용
  private volatile Map<Integer, String> movieGenreCache;
  private volatile Map<Integer, String> tvGenreCache;

  /**
   * RestClient 생성
   * 매 요청마다 Authorization 헤더 자동 추가
   */
  private RestClient restClient() {
    return RestClient.builder()
        .baseUrl(tmdbProperties.getBaseUrl())
        .defaultHeader(
            HttpHeaders.AUTHORIZATION,
            "Bearer " + tmdbProperties.getAccessToken()
        )
        .defaultHeader("accept", "application/json")
        .build();
  }


  // GET /movie/popular?language=ko-KR&page={page}
  public List<TmdbMovieResult> fetchPopularMovies(int page) {
    TmdbPageResponse<TmdbMovieResult> response = externalApiRetryTemplate.execute(ctx -> {
      log.info("[TMDB] 인기 영화 수집 - page: {}", page);

      return restClient()
          .get()
          .uri(uriBuilder -> uriBuilder
              .path("/movie/popular")
              .queryParam("language", "ko-KR")
              .queryParam("page", page)
              .build()
          )
          .retrieve()
          // 429: TMDB 요청 한도 초과 - 재시도 대상
          .onStatus(status -> status.value() == 429, (req, res) -> {
            log.warn("[TMDB] 영화 API 요청 한도 초과 - page: {}", page);
            throw new ExternalApiRetryableException(ErrorCode.TMDB_RATE_LIMITED);
          })
          // 4xx: 잘못된 API 키, 파라미터 오류 등 - 재시도해도 결과가 같으므로 즉시 실패
          .onStatus(status -> status.is4xxClientError(), (req, res) -> {
            log.error("[TMDB] 영화 API 클라이언트 오류 - status: {}", res.getStatusCode());
            throw new MoplException(ErrorCode.TMDB_CLIENT_ERROR);
          })
          // 5xx: TMDB 서버 오류 - 재시도 대상
          .onStatus(status -> status.is5xxServerError(), (req, res) -> {
            log.error("[TMDB] 영화 API 서버 오류 - status: {}", res.getStatusCode());
            throw new ExternalApiRetryableException(ErrorCode.TMDB_SERVER_ERROR);
          })
          .body(new ParameterizedTypeReference<>() {});
    });

    return response != null ? response.results() : List.of();
  }


  //GET /tv/popular?language=ko-KR&page={page}
  public List<TmdbTvResult> fetchPopularTv(int page) {
    TmdbPageResponse<TmdbTvResult> response = externalApiRetryTemplate.execute(ctx -> {
      log.info("[TMDB] 인기 TV시리즈 수집 - page: {}", page);

      return restClient()
          .get()
          .uri(uriBuilder -> uriBuilder
              .path("/tv/popular")
              .queryParam("language", "ko-KR")
              .queryParam("page", page)
              .build()
          )
          .retrieve()
          .onStatus(status -> status.value() == 429, (req, res) -> {
            log.warn("[TMDB] TV시리즈 API 요청 한도 초과 - page: {}", page);
            throw new ExternalApiRetryableException(ErrorCode.TMDB_RATE_LIMITED);
          })
          .onStatus(status -> status.is4xxClientError(), (req, res) -> {
            log.error("[TMDB] TV시리즈 API 클라이언트 오류 - status: {}", res.getStatusCode());
            throw new MoplException(ErrorCode.TMDB_CLIENT_ERROR);
          })
          .onStatus(status -> status.is5xxServerError(), (req, res) -> {
            log.error("[TMDB] TV시리즈 API 서버 오류 - status: {}", res.getStatusCode());
            throw new ExternalApiRetryableException(ErrorCode.TMDB_SERVER_ERROR);
          })
          .body(new ParameterizedTypeReference<>() {});
    });

    return response != null ? response.results() : List.of();
  }

  /** 영화 장르 ID→이름 맵 (id: 28 → "액션" 등). 최초 호출 시 조회 후 캐싱 */
  public synchronized Map<Integer, String> getMovieGenres() {
    if (movieGenreCache == null) {
      movieGenreCache = fetchGenres("/genre/movie/list");
    }
    return movieGenreCache;
  }

  /** TV 장르 ID→이름 맵. 최초 호출 시 조회 후 캐싱 */
  public synchronized Map<Integer, String> getTvGenres() {
    if (tvGenreCache == null) {
      tvGenreCache = fetchGenres("/genre/tv/list");
    }
    return tvGenreCache;
  }

  // GET /genre/movie/list 또는 /genre/tv/list ?language=ko-KR
  private Map<Integer, String> fetchGenres(String path) {
    TmdbGenreListResponse response = externalApiRetryTemplate.execute(ctx -> {
      log.info("[TMDB] 장르 목록 조회 - path: {}", path);

      return restClient()
          .get()
          .uri(uriBuilder -> uriBuilder
              .path(path)
              .queryParam("language", "ko-KR")
              .build()
          )
          .retrieve()
          .onStatus(status -> status.value() == 429, (req, res) -> {
            log.warn("[TMDB] 장르 목록 API 요청 한도 초과 - path: {}", path);
            throw new ExternalApiRetryableException(ErrorCode.TMDB_RATE_LIMITED);
          })
          .onStatus(status -> status.is4xxClientError(), (req, res) -> {
            log.error("[TMDB] 장르 목록 API 클라이언트 오류 - status: {}", res.getStatusCode());
            throw new MoplException(ErrorCode.TMDB_CLIENT_ERROR);
          })
          .onStatus(status -> status.is5xxServerError(), (req, res) -> {
            log.error("[TMDB] 장르 목록 API 서버 오류 - status: {}", res.getStatusCode());
            throw new ExternalApiRetryableException(ErrorCode.TMDB_SERVER_ERROR);
          })
          .body(TmdbGenreListResponse.class);
    });

    if (response == null || response.genres() == null) {
      return Map.of();
    }
    return response.genres().stream()
        .collect(Collectors.toMap(TmdbResponse.TmdbGenre::id, TmdbResponse.TmdbGenre::name));
  }

  /**
   * 포스터 이미지 전체 URL 생성
   * TMDB는 이미지 경로만 주고 baseUrl을 붙여야 함
   * 예: /abc123.jpg → https://image.tmdb.org/t/p/w500/abc123.jpg
   */
  public String buildImageUrl(String posterPath) {
    if (posterPath == null) return null;
    return tmdbProperties.getImageBaseUrl() + posterPath;
  }
}