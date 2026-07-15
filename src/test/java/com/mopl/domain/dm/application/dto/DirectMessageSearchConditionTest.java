package com.mopl.domain.dm.application.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mopl.global.dto.SortDirection;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DirectMessageSearchConditionTest {

  @Test
  @DisplayName("첫 페이지이면 true를 반환한다")
  void isFirstPage_true() {
    DirectMessageSearchCondition condition =
        new DirectMessageSearchCondition(
            null,
            null,
            10,
            SortDirection.DESCENDING,
            "createdAt"
        );

    assertThat(condition.isFirstPage()).isTrue();
  }

  @Test
  @DisplayName("cursor가 존재하면 false를 반환한다")
  void isFirstPage_false_whenCursorExists() {
    DirectMessageSearchCondition condition =
        new DirectMessageSearchCondition(
            "2026-01-01T00:00:00Z",
            null,
            10,
            SortDirection.DESCENDING,
            "createdAt"
        );

    assertThat(condition.isFirstPage()).isFalse();
  }

  @Test
  @DisplayName("idAfter가 존재하면 false를 반환한다")
  void isFirstPage_false_whenIdAfterExists() {
    DirectMessageSearchCondition condition =
        new DirectMessageSearchCondition(
            null,
            UUID.randomUUID(),
            10,
            SortDirection.DESCENDING,
            "createdAt"
        );

    assertThat(condition.isFirstPage()).isFalse();
  }
}