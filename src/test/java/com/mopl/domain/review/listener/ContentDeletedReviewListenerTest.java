package com.mopl.domain.review.listener;

import static org.mockito.BDDMockito.then;

import com.mopl.domain.review.repository.ReviewRepository;
import com.mopl.global.event.ContentDeletedEvent;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ContentDeletedReviewListenerTest {

  @Mock
  private ReviewRepository reviewRepository;

  @InjectMocks
  private ContentDeletedReviewListener listener;

  @Test
  @DisplayName("handle: 이벤트의 contentId로 해당 콘텐츠의 모든 리뷰를 삭제한다")
  void handle_deletesReviewsByContentId() {
    UUID contentId = UUID.randomUUID();
    ContentDeletedEvent event = new ContentDeletedEvent(contentId);

    listener.handle(event);

    then(reviewRepository).should().deleteByContentId(contentId);
  }
}