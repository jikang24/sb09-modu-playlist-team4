package com.mopl.domain.content.event;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;

import com.mopl.domain.content.adapter.port.SearchContentPort;
import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.global.event.ReviewRatingUpdatedEvent;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReviewRatingUpdatedEventListener 테스트")
class ReviewRatingUpdatedEventListenerTest {

  @Mock
  private ContentRepository contentRepository;

  @Mock
  private SearchContentPort searchContentPort;

  @InjectMocks
  private ReviewRatingUpdatedEventListener listener;

  private Content makeContent(UUID id) {
    return Content.restore(
        id,
        ContentType.MOVIE,
        "tmdb-001",
        "테스트",
        "설명",
        null,
        BigDecimal.ZERO,
        0,
        Instant.now(),
        Instant.now(),
        List.of()
    );
  }

  @Test
  @DisplayName("정상 처리 - delta만큼 원자적으로 갱신하고, 반영된 최신값을 검색 인덱스에 동기화한다")
  void success() {
    UUID id = UUID.randomUUID();
    BigDecimal ratingDelta = new BigDecimal("4.50");

    Content content = makeContent(id);

    ReviewRatingUpdatedEvent event = new ReviewRatingUpdatedEvent(id, ratingDelta, 1);

    given(contentRepository.findById(id)).willReturn(Optional.of(content));

    listener.handleReviewRatingUpdated(event);

    then(contentRepository).should().applyRatingDelta(id, ratingDelta, 1);
    then(contentRepository).should().findById(id);
    then(searchContentPort).should().save(content);
  }

  @Test
  @DisplayName("검색 색인 반영 중 예외가 나도 조용히 무시되고 전파되지 않는다")
  void success_searchSyncFailureSwallowed() {
    UUID id = UUID.randomUUID();
    BigDecimal ratingDelta = new BigDecimal("4.50");

    Content content = makeContent(id);

    ReviewRatingUpdatedEvent event = new ReviewRatingUpdatedEvent(id, ratingDelta, 1);

    given(contentRepository.findById(id)).willReturn(Optional.of(content));
    willThrow(new RuntimeException("opensearch down"))
        .given(searchContentPort).save(content);

    listener.handleReviewRatingUpdated(event);

    then(contentRepository).should().applyRatingDelta(id, ratingDelta, 1);
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠 - 예외를 전파하지 않고 삼킨다 (@Async라 던져봐야 호출자에게 안 감 - 로그로 남기고 조용히 종료)")
  void fail_notFound_swallowedNotPropagated() {
    UUID id = UUID.randomUUID();

    ReviewRatingUpdatedEvent event =
        new ReviewRatingUpdatedEvent(id, new BigDecimal("3.00"), 5);

    given(contentRepository.findById(id)).willReturn(Optional.empty());

    assertThatCode(() -> listener.handleReviewRatingUpdated(event)).doesNotThrowAnyException();

    then(searchContentPort).should(never()).save(any());
  }
}