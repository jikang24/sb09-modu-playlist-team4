package com.mopl.infra.sportsdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
import com.mopl.infra.common.ExternalApiRetryableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * SportsDB API 클라이언트
 *
 * 무료 플랜: API 키 "3" 사용
 * 프로토 타입에 축구경기가 많아 임시 반영
 * EPL 리그 ID: 4328
 * 시즌: 2024-2025
 *
 * 429/5xx는 externalApiRetryTemplate으로 지수 백오프 재시도, 그 외 4xx는 즉시 실패 처리
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SportsDbClient {

  private static final String BASE_URL = "https://www.thesportsdb.com/api/v1/json/3";
  private static final String EPL_LEAGUE_ID = "4328";
  private static final String CURRENT_SEASON = "2024-2025";

  private final RetryTemplate externalApiRetryTemplate;

  /**
   * SportsDB 경기 응답 DTO
   */
  public record SportsDbEventResponse(
      List<SportsDbEvent> events
  ) {}

  public record SportsDbEvent(
      @JsonProperty("idEvent") String idEvent,
      @JsonProperty("strEvent") String strEvent,
      @JsonProperty("strLeague") String strLeague,
      @JsonProperty("strHomeTeam") String strHomeTeam,  // 홈팀
      @JsonProperty("strAwayTeam") String strAwayTeam,  // 원정팀
      @JsonProperty("dateEvent") String dateEvent,      // 경기 날짜
      @JsonProperty("strThumb") String strThumb,
      // 경기 썸네일(strThumb)이 비어있는 경우의 대체 후보. eventsseason.php 응답에 이미
      // 포함되어 있어 추가 API 호출 없이 그대로 파싱만 하면 된다.
      @JsonProperty("strHomeTeamBadge") String strHomeTeamBadge,
      @JsonProperty("strAwayTeamBadge") String strAwayTeamBadge,
      @JsonProperty("strLeagueBadge") String strLeagueBadge
  ) {

    /**
     * 썸네일 후보 우선순위: 경기 썸네일 -> 홈팀 배지 -> 원정팀 배지 -> 리그 배지 -> 없음
     * 전부 없으면 null을 반환하며, 이 경우 프론트 fallback 요청은 PlaceholderImageController가 처리한다.
     */
    public String resolveThumbnailUrl() {
      return Stream.of(strThumb, strHomeTeamBadge, strAwayTeamBadge, strLeagueBadge)
          .map(SportsDbEvent::nullIfBlank)
          .filter(Objects::nonNull)
          .findFirst()
          .orElse(null);
    }

    private static String nullIfBlank(String value) {
      return (value == null || value.isBlank()) ? null : value;
    }
  }

  private RestClient restClient() {
    return RestClient.builder()
        .baseUrl(BASE_URL)
        .defaultHeader("accept", "application/json")
        .build();
  }

  /**
   * EPL 시즌 경기 목록 수집
   * GET /eventsseason.php?id={leagueId}&s={season}
   */
  public List<SportsDbEvent> fetchEplEvents() {
    SportsDbEventResponse response = externalApiRetryTemplate.execute(ctx -> {
      log.info("[SportsDB] EPL 경기 수집 - 시즌: {}", CURRENT_SEASON);

      return restClient()
          .get()
          .uri(uriBuilder -> uriBuilder
              .path("/eventsseason.php")
              .queryParam("id", EPL_LEAGUE_ID)
              .queryParam("s", CURRENT_SEASON)
              .build()
          )
          .retrieve()
          .onStatus(status -> status.value() == 429, (req, res) -> {
            log.warn("[SportsDB] EPL 경기 API 요청 한도 초과");
            throw new ExternalApiRetryableException(ErrorCode.SPORTSDB_RATE_LIMITED);
          })
          .onStatus(status -> status.is4xxClientError(), (req, res) -> {
            log.error("[SportsDB] EPL 경기 API 클라이언트 오류 - status: {}", res.getStatusCode());
            throw new MoplException(ErrorCode.SPORTSDB_CLIENT_ERROR);
          })
          .onStatus(status -> status.is5xxServerError(), (req, res) -> {
            log.error("[SportsDB] EPL 경기 API 서버 오류 - status: {}", res.getStatusCode());
            throw new ExternalApiRetryableException(ErrorCode.SPORTSDB_SERVER_ERROR);
          })
          .body(SportsDbEventResponse.class);
    });

    return response != null && response.events() != null
        ? response.events()
        : List.of();
  }
}