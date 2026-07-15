package com.mopl.domain.review.listener;

import static org.mockito.BDDMockito.then;

import com.mopl.domain.review.repository.ReviewRepository;
import com.mopl.global.event.UserProfileUpdatedEvent;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewAuthorSyncListenerTest {

  @Mock
  private ReviewRepository reviewRepository;

  @InjectMocks
  private ReviewAuthorSyncListener listener;

  @Test
  @DisplayName("handle: 이벤트의 새 이름/프로필사진으로 해당 유저의 모든 리뷰 작성자 스냅샷을 갱신한다")
  void handle_updatesAuthorSnapshot() {
    UUID userId = UUID.randomUUID();
    UserProfileUpdatedEvent event = new UserProfileUpdatedEvent(userId, "새이름", "https://new.jpg");

    listener.handle(event);

    then(reviewRepository).should()
        .updateAuthorSnapshotByUserId(userId, "새이름", "https://new.jpg");
  }
}