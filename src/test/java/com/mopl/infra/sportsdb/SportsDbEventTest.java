package com.mopl.infra.sportsdb;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopl.infra.sportsdb.SportsDbClient.SportsDbEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SportsDbEventTest {

  @Test
  @DisplayName("strThumb이 있으면 그대로 사용한다")
  void resolveThumbnailUrl_usesEventThumb() {
    SportsDbEvent event = event("thumb.png", "home.png", "away.png", "league.png");

    assertThat(event.resolveThumbnailUrl()).isEqualTo("thumb.png");
  }

  @Test
  @DisplayName("strThumb이 없으면 홈팀 배지로 대체한다")
  void resolveThumbnailUrl_fallsBackToHomeTeamBadge() {
    SportsDbEvent event = event(null, "home.png", "away.png", "league.png");

    assertThat(event.resolveThumbnailUrl()).isEqualTo("home.png");
  }

  @Test
  @DisplayName("경기/홈팀 썸네일이 없으면 원정팀 배지로 대체한다")
  void resolveThumbnailUrl_fallsBackToAwayTeamBadge() {
    SportsDbEvent event = event("", "", "away.png", "league.png");

    assertThat(event.resolveThumbnailUrl()).isEqualTo("away.png");
  }

  @Test
  @DisplayName("모든 팀 썸네일이 없으면 리그 배지로 대체한다")
  void resolveThumbnailUrl_fallsBackToLeagueBadge() {
    SportsDbEvent event = event(null, null, null, "league.png");

    assertThat(event.resolveThumbnailUrl()).isEqualTo("league.png");
  }

  @Test
  @DisplayName("모든 후보가 없으면 null을 반환한다 (프론트 fallback 경로로 처리됨)")
  void resolveThumbnailUrl_returnsNullWhenNoCandidate() {
    SportsDbEvent event = event(null, "  ", "", null);

    assertThat(event.resolveThumbnailUrl()).isNull();
  }

  private SportsDbEvent event(String thumb, String homeBadge, String awayBadge, String leagueBadge) {
    return new SportsDbEvent(
        "1", "Home vs Away", "EPL", "Home", "Away", "2025-01-01",
        thumb, homeBadge, awayBadge, leagueBadge);
  }
}