package com.mopl.domain.watchingsession.adapter.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.dto.ContentResponse;
import com.mopl.domain.content.service.ContentUseCase;
import com.mopl.global.dto.ContentSummary;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WatchingSessionContentAdapterTest {

  @Mock
  private ContentUseCase contentUseCase;

  @InjectMocks
  private WatchingSessionContentAdapter adapter;

  @Test
  @DisplayName("getContent: ContentUseCase 조회 결과를 ContentSummary로 변환해서 반환한다")
  void getContent_mapsContentResponseToSummary() {
    UUID contentId = UUID.randomUUID();
    ContentResponse response = new ContentResponse(
        contentId, ContentType.MOVIE, "제목", "설명", "https://thumb.jpg",
        List.of("액션", "SF"), new BigDecimal("4.50"), 12, 3L
    );
    given(contentUseCase.getContent(contentId)).willReturn(response);

    ContentSummary summary = adapter.getContent(contentId);

    assertThat(summary).isEqualTo(new ContentSummary(
        contentId, ContentType.MOVIE, "제목", "설명", "https://thumb.jpg",
        List.of("액션", "SF"), new BigDecimal("4.50"), 12
    ));
  }
}