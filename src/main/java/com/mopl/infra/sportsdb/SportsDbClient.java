package com.mopl.infra.sportsdb;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * SportsDB API 클라이언트
 *
 * 무료 플랜: API 키 "3" 사용
 * 프로토 타입에 축구경기가 많아 임시 반영
 * EPL 리그 ID: 4328
 * 시즌: 2024-2025
 */
@Slf4j
@Component
public class SportsDbClient {

  private static final String BASE_URL = "https://www.thesportsdb.com/api/v1/json/3";
  private static final String EPL_LEAGUE_ID = "4328";
  private static final String CURRENT_SEASON = "2024-2025";

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
      @JsonProperty("strThumb") String strThumb
  ) {}

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
    log.info("[SportsDB] EPL 경기 수집 - 시즌: {}", CURRENT_SEASON);

    SportsDbEventResponse response = restClient()
        .get()
        .uri(uriBuilder -> uriBuilder
            .path("/eventsseason.php")
            .queryParam("id", EPL_LEAGUE_ID)
            .queryParam("s", CURRENT_SEASON)
            .build()
        )
        .retrieve()
        .body(SportsDbEventResponse.class);

    return response != null && response.events() != null
        ? response.events()
        : List.of();
  }
}