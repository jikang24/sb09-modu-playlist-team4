package com.mopl.domain.review.listener;

import com.mopl.domain.review.repository.ReviewRepository;
import com.mopl.global.event.UserProfileUpdatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * User가 프로필(이름/사진)을 바꿨을 때, 이 유저가 쓴 모든 리뷰의 author 스냅샷을 갱신
 * @Async + AFTER_COMMIT: User 프로필 변경 트랜잭션이 커밋된 후, 별도 스레드에서 처리
 */
@Component
@RequiredArgsConstructor
public class ReviewAuthorSyncListener {

  private final ReviewRepository reviewRepository;

  @Async
  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void handle(UserProfileUpdatedEvent event) {
    reviewRepository.updateAuthorSnapshotByUserId(
        event.userId(), event.newName(), event.newProfileImageUrl());
  }
}