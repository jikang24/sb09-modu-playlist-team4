package com.mopl.domain.content.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.mopl.domain.content.adapter.port.SearchContentPort;
import com.mopl.domain.content.domain.Content;
import com.mopl.domain.content.domain.ContentType;
import com.mopl.domain.content.repository.ContentRepository;
import com.mopl.global.event.ReviewRatingUpdatedEvent;
import com.mopl.global.exception.ErrorCode;
import com.mopl.global.exception.MoplException;
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
  @DisplayName("정상 처리 - 평점/리뷰수 갱신 후 저장")
  void success() {
    UUID id = UUID.randomUUID();

    Content content = makeContent(id);

    ReviewRatingUpdatedEvent event =
        new ReviewRatingUpdatedEvent(id, new BigDecimal("4.50"), 15);

    given(contentRepository.findById(id)).willReturn(Optional.of(content));
    given(contentRepository.save(any(Content.class))).willReturn(content);

    listener.handleReviewRatingUpdated(event);

    then(contentRepository).should().findById(id);
    then(contentRepository).should().save(any(Content.class));
    then(searchContentPort).should().save(content);
  }

  @Test
  @DisplayName("존재하지 않는 콘텐츠 - CONTENT_NOT_FOUND")
  void fail_notFound() {
    UUID id = UUID.randomUUID();

    ReviewRatingUpdatedEvent event =
        new ReviewRatingUpdatedEvent(id, new BigDecimal("3.00"), 5);

    given(contentRepository.findById(id)).willReturn(Optional.empty());

    assertThatThrownBy(() -> listener.handleReviewRatingUpdated(event))
        .isInstanceOf(MoplException.class)
        .satisfies(e ->
            assertThat(((MoplException) e).getErrorCode())
                .isEqualTo(ErrorCode.CONTENT_NOT_FOUND));
  }
}