package com.mopl.infra.tmdb;

import com.mopl.infra.tmdb.TmdbResponse.TmdbMovieResult;
import com.mopl.infra.tmdb.TmdbResponse.TmdbPageResponse;
import com.mopl.infra.tmdb.TmdbResponse.TmdbTvResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * TMDB API 클라이언트
 *
 * RestClient: RestTemplate 대체, 요청조립 등에서 이점(동기 방식)
 * 나중에 병목 지점이 보이면 그 부분만 WebClient로 교체하는 식 고려
 *
 * 인증 방식: Bearer Token (API 읽기 액세스 토큰)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TmdbClient {

  private final TmdbProperties tmdbProperties;

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
    log.info("[TMDB] 인기 영화 수집 - page: {}", page);

    TmdbPageResponse<TmdbMovieResult> response = restClient()
        .get()
        .uri(uriBuilder -> uriBuilder
            .path("/movie/popular")
            .queryParam("language", "ko-KR")
            .queryParam("page", page)
            .build()
        )
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});

    return response != null ? response.results() : List.of();
  }


  //GET /tv/popular?language=ko-KR&page={page}
  public List<TmdbTvResult> fetchPopularTv(int page) {
    log.info("[TMDB] 인기 TV시리즈 수집 - page: {}", page);

    TmdbPageResponse<TmdbTvResult> response = restClient()
        .get()
        .uri(uriBuilder -> uriBuilder
            .path("/tv/popular")
            .queryParam("language", "ko-KR")
            .queryParam("page", page)
            .build()
        )
        .retrieve()
        .body(new ParameterizedTypeReference<>() {});

    return response != null ? response.results() : List.of();
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
